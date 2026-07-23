import { TestBed } from '@angular/core/testing';
import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  beforeEach(() => sessionStorage.clear());

  it('establishes, restores, and clears a session through one storage boundary', () => {
    const first = TestBed.inject(AuthSessionService);
    first.establish('access-token', { email: 'user@example.com' });
    expect(first.authenticated()).toBeTrue();
    expect(first.accessToken()).toBe('access-token');

    first.clear();
    expect(first.authenticated()).toBeFalse();
    expect(sessionStorage.length).toBe(0);
  });
});
