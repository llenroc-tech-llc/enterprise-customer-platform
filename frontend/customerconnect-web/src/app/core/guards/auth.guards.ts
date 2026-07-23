import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSessionService } from '../auth/auth-session.service';

export const authGuard: CanActivateFn = () =>
  inject(AuthSessionService).authenticated() ? true : inject(Router).createUrlTree(['/auth/login']);

export const guestGuard: CanActivateFn = () =>
  inject(AuthSessionService).authenticated() ? inject(Router).createUrlTree(['/dashboard']) : true;

export const permissionGuard =
  (permission: string): CanActivateFn =>
  () => {
    // The current safe user contract does not expose permissions yet.
    void permission;
    return inject(AuthSessionService).authenticated()
      ? true
      : inject(Router).createUrlTree(['/auth/login']);
  };
