import { computed, Injectable, signal } from '@angular/core';
import { UserSummary } from '../models/api.models';

const SESSION_KEY = 'llenroc.customerconnect.session';

interface StoredSession {
  accessToken: string;
  user: UserSummary;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly session = signal<StoredSession | null>(this.restore());
  readonly user = computed(() => this.session()?.user ?? null);
  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly authenticated = computed(() => Boolean(this.session()?.accessToken));

  establish(accessToken: string, user: UserSummary): void {
    const session = { accessToken, user };
    this.session.set(session);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  updateAccessToken(accessToken: string): void {
    const current = this.session();
    if (current) this.establish(accessToken, current.user);
  }

  clear(): void {
    this.session.set(null);
    sessionStorage.removeItem(SESSION_KEY);
  }

  private restore(): StoredSession | null {
    try {
      const value = sessionStorage.getItem(SESSION_KEY);
      return value ? (JSON.parse(value) as StoredSession) : null;
    } catch {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
