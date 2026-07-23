import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthApiService } from '../api/auth-api.service';
import { AuthSessionService } from '../auth/auth-session.service';

let refreshInProgress = false;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthSessionService);
  const api = inject(AuthApiService);
  const router = inject(Router);
  const token = session.accessToken();
  const isRefresh = request.url.endsWith('/user/refresh-token');
  const authorizedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true })
    : request.clone({ withCredentials: true });

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isRefresh || !session.authenticated() || refreshInProgress) {
        return throwError(() => error);
      }
      refreshInProgress = true;
      return api.refresh().pipe(
        switchMap((response) => {
          refreshInProgress = false;
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
          refreshInProgress = false;
          session.clear();
          void router.navigate(['/auth/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
