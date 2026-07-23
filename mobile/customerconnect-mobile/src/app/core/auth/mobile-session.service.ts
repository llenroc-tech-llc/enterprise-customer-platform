import { computed, inject, Injectable, signal } from '@angular/core';
import { StoredSession, UserSummary } from '../models/api.models';
import { SESSION_STORAGE } from '../storage/session-storage';

const KEY = 'llenroc.mobile.session';

@Injectable({ providedIn: 'root' })
export class MobileSessionService {
  private readonly storage = inject(SESSION_STORAGE);
  private readonly session = signal<StoredSession | null>(this.storage.read<StoredSession>(KEY));
  readonly authenticated = computed(() => Boolean(this.session()?.accessToken));
  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly user = computed(() => this.session()?.user ?? null);

  establish(accessToken: string, user: UserSummary): void {
    const session = { accessToken, user };
    this.session.set(session);
    this.storage.write(KEY, session);
  }
  updateAccessToken(accessToken: string): void {
    const current = this.session();
    if (current) this.establish(accessToken, current.user);
  }
  clear(): void {
    this.session.set(null);
    this.storage.remove(KEY);
  }
}
