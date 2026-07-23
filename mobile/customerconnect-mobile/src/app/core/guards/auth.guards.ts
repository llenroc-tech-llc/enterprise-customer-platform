import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MobileSessionService } from '../auth/mobile-session.service';
export const authGuard: CanActivateFn = () =>
  inject(MobileSessionService).authenticated()
    ? true
    : inject(Router).createUrlTree(['/auth/login']);
export const guestGuard: CanActivateFn = () =>
  inject(MobileSessionService).authenticated()
    ? inject(Router).createUrlTree(['/tabs/home'])
    : true;
