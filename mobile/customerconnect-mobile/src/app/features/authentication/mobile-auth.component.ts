import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  IonButton,
  IonContent,
  IonInput,
  IonItem,
  IonLabel,
  IonSpinner,
} from '@ionic/angular/standalone';
import { finalize } from 'rxjs';
import { MobileAuthApiService } from '../../core/api/mobile-auth-api.service';
import { MobileSessionService } from '../../core/auth/mobile-session.service';
import { ApiResponse, AuthenticationData } from '../../core/models/api.models';

type Mode = 'login' | 'mfa' | 'register' | 'verify-account' | 'forgot-password' | 'reset-password';

@Component({
  selector: 'app-mobile-auth',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonItem,
    IonInput,
    IonLabel,
    IonButton,
    IonSpinner,
  ],
  templateUrl: './mobile-auth.component.html',
  styleUrl: './mobile-auth.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileAuthComponent {
  private readonly builder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(MobileAuthApiService);
  private readonly session = inject(MobileSessionService);
  readonly mode = signal<Mode>('login');
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly linkState = signal<'idle' | 'loading' | 'valid' | 'invalid'>('idle');
  readonly title = computed(
    () =>
      ({
        login: 'Welcome back',
        mfa: 'Verify your identity',
        register: 'Create your account',
        'verify-account': 'Verify your account',
        'forgot-password': 'Forgot password',
        'reset-password': 'New password',
      })[this.mode()],
  );
  readonly form = this.builder.nonNullable.group({
    firstName: [''],
    lastName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    confirmPassword: [''],
    code: [''],
  });

  constructor() {
    this.route.paramMap.subscribe((params) => {
      this.mode.set((params.get('mode') ?? 'login') as Mode);
      const email = history.state.email as string | undefined;
      if (email) this.form.controls.email.setValue(email);
      if (this.mode() === 'verify-account') this.checkAccountLink();
      if (this.mode() === 'reset-password') this.checkResetLink();
    });
  }

  submit(): void {
    const value = this.form.getRawValue();
    this.error.set('');
    if (!this.isValid(value)) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const operation = this.operation(value);
    if (!operation) {
      this.loading.set(false);
      return;
    }
    operation.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => this.success(response),
      error: (error: HttpErrorResponse) =>
        this.error.set(
          (error.error as Partial<ApiResponse>)?.message ?? 'Unable to complete the request.',
        ),
    });
  }

  private operation(value: ReturnType<typeof this.form.getRawValue>) {
    if (this.mode() === 'login') return this.api.login(value.email, value.password);
    if (this.mode() === 'mfa') return this.api.verifyMfa(value.email, value.code);
    if (this.mode() === 'register') return this.api.register(value);
    if (this.mode() === 'forgot-password') return this.api.forgotPassword(value.email);
    if (this.mode() === 'reset-password')
      return this.api.resetPassword(
        this.route.snapshot.queryParamMap.get('token') ?? '',
        value.password,
        value.confirmPassword,
      );
    return null;
  }

  private success(response: ApiResponse): void {
    if (this.mode() === 'login' || this.mode() === 'mfa') {
      const data = response.data as AuthenticationData | undefined;
      if (data?.accessToken && data.user) {
        this.session.establish(data.accessToken, data.user);
        void this.router.navigate(['/tabs/home']);
      } else {
        void this.router.navigate(['/auth/mfa'], {
          state: { email: this.form.controls.email.value },
        });
      }
      return;
    }
    this.message.set(
      this.mode() === 'forgot-password'
        ? 'If an account exists for this email address, password reset instructions will be sent.'
        : response.message,
    );
  }

  private checkAccountLink(): void {
    const key = this.route.snapshot.queryParamMap.get('key') ?? '';
    if (!key) {
      this.linkState.set('invalid');
      return;
    }
    this.linkState.set('loading');
    this.api.verifyAccount(key).subscribe({
      next: (response) => {
        this.linkState.set('valid');
        this.message.set(response.message);
      },
      error: () => this.linkState.set('invalid'),
    });
  }

  private checkResetLink(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.linkState.set('invalid');
      return;
    }
    this.linkState.set('loading');
    this.api.verifyReset(token).subscribe({
      next: () => this.linkState.set('valid'),
      error: () => this.linkState.set('invalid'),
    });
  }

  private isValid(value: ReturnType<typeof this.form.getRawValue>): boolean {
    if (this.mode() !== 'reset-password' && this.form.controls.email.invalid) return false;
    if (this.mode() === 'login') return Boolean(value.password);
    if (this.mode() === 'mfa') return value.code.trim().length >= 6;
    if (this.mode() === 'register')
      return Boolean(
        value.firstName &&
        value.lastName &&
        value.password &&
        value.password === value.confirmPassword,
      );
    if (this.mode() === 'reset-password')
      return Boolean(value.password && value.password === value.confirmPassword);
    return true;
  }
}
