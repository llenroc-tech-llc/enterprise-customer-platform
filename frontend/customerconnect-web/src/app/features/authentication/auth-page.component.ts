import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../../core/api/auth-api.service';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { ApiResponse, AuthenticationData } from '../../core/models/api.models';

type AuthMode =
  'login' | 'mfa' | 'register' | 'verify-account' | 'forgot-password' | 'reset-password';

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AuthApiService);
  private readonly session = inject(AuthSessionService);

  readonly mode = signal<AuthMode>('login');
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly verificationState = signal<'idle' | 'verifying' | 'verified' | 'invalid'>('idle');
  readonly title = computed(() => {
    const titles: Record<AuthMode, string> = {
      login: 'Welcome back',
      mfa: 'Verify your identity',
      register: 'Create your workspace account',
      'verify-account': 'Verify your account',
      'forgot-password': 'Reset your password',
      'reset-password': 'Choose a new password',
    };
    return titles[this.mode()];
  });

  readonly form = this.formBuilder.nonNullable.group({
    firstName: [''],
    lastName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    confirmPassword: [''],
    code: [''],
  });

  constructor() {
    this.route.paramMap.subscribe((parameters) => {
      this.mode.set((parameters.get('mode') ?? 'login') as AuthMode);
      this.error.set('');
      this.message.set('');
      const email = history.state.email as string | undefined;
      if (email) this.form.controls.email.setValue(email);
      if (this.mode() === 'verify-account') this.verifyAccount();
      if (this.mode() === 'reset-password') this.verifyResetToken();
    });
  }

  submit(): void {
    this.error.set('');
    this.message.set('');
    if (!this.validForMode()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const value = this.form.getRawValue();
    const request = this.requestForMode(value);
    if (!request) {
      this.loading.set(false);
      return;
    }
    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (response) => this.handleSuccess(response),
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  private requestForMode(value: ReturnType<typeof this.form.getRawValue>) {
    switch (this.mode()) {
      case 'login':
        return this.api.login({ email: value.email, password: value.password });
      case 'mfa':
        return this.api.verifyMfa(value.email, value.code);
      case 'register':
        return this.api.register({
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          password: value.password,
        });
      case 'forgot-password':
        return this.api.requestPasswordReset(value.email);
      case 'reset-password':
        return this.api.resetPassword({
          token: this.route.snapshot.queryParamMap.get('token') ?? '',
          password: value.password,
          confirmPassword: value.confirmPassword,
        });
      default:
        return null;
    }
  }

  private handleSuccess(response: ApiResponse<unknown>): void {
    if (this.mode() === 'login') {
      const data = response.data as AuthenticationData | undefined;
      if (data?.accessToken && data.user) {
        this.session.establish(data.accessToken, data.user);
        void this.router.navigate(['/dashboard']);
      } else {
        void this.router.navigate(['/auth/mfa'], {
          state: { email: this.form.controls.email.value },
        });
      }
      return;
    }
    if (this.mode() === 'mfa') {
      const data = response.data as AuthenticationData | undefined;
      if (data?.accessToken && data.user) {
        this.session.establish(data.accessToken, data.user);
        void this.router.navigate(['/dashboard']);
      }
      return;
    }
    this.message.set(
      this.mode() === 'forgot-password'
        ? 'If an account exists for this email address, password reset instructions will be sent.'
        : response.message,
    );
  }

  private handleError(error: HttpErrorResponse): void {
    const response = error.error as Partial<ApiResponse> | undefined;
    this.error.set(response?.message ?? 'We could not complete that request. Please try again.');
  }

  private verifyAccount(): void {
    const key = this.route.snapshot.queryParamMap.get('key') ?? '';
    if (!key) {
      this.verificationState.set('invalid');
      return;
    }
    this.verificationState.set('verifying');
    this.api.verifyAccount(key).subscribe({
      next: (response) => {
        this.verificationState.set('verified');
        this.message.set(response.message);
      },
      error: () => this.verificationState.set('invalid'),
    });
  }

  private verifyResetToken(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.verificationState.set('invalid');
      return;
    }
    this.verificationState.set('verifying');
    this.api.verifyResetToken(token).subscribe({
      next: () => this.verificationState.set('verified'),
      error: () => this.verificationState.set('invalid'),
    });
  }

  private validForMode(): boolean {
    const value = this.form.getRawValue();
    if (['login', 'register', 'forgot-password', 'mfa'].includes(this.mode())) {
      if (this.form.controls.email.invalid) return false;
    }
    if (['login', 'register', 'reset-password'].includes(this.mode()) && !value.password) {
      return false;
    }
    if (this.mode() === 'register') {
      return Boolean(value.firstName && value.lastName && value.password === value.confirmPassword);
    }
    if (this.mode() === 'mfa') return value.code.trim().length >= 6;
    if (this.mode() === 'reset-password') return value.password === value.confirmPassword;
    return true;
  }
}
