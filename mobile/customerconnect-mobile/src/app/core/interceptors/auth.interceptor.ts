import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { MobileAuthApiService } from '../api/mobile-auth-api.service';
import { MobileSessionService } from '../auth/mobile-session.service';

let refreshing = false;
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(MobileSessionService);
  const api = inject(MobileAuthApiService);
  const router = inject(Router);
  const token = session.accessToken();
  const refreshRequest = request.url.endsWith('/user/refresh-token');
  const outgoing = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true })
    : request.clone({ withCredentials: true });
  return next(outgoing).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || refreshRequest || !session.authenticated() || refreshing)
        return throwError(() => error);
      refreshing = true;
      return api.refresh().pipe(
        switchMap((response) => {
          refreshing = false;
          const accessToken = response.data?.accessToken;
          if (!accessToken) return throwError(() => error);
          session.updateAccessToken(accessToken);
          return next(
            request.clone({
              setHeaders: { Authorization: `Bearer ${accessToken}` },
              withCredentials: true,
            }),
          );
        }),
        catchError((refreshError: HttpErrorResponse) => {
          refreshing = false;
          session.clear();
          void router.navigate(['/auth/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
