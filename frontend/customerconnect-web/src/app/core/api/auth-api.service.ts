import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../configuration/api.config';
import {
  ApiResponse,
  AuthenticationData,
  LoginRequest,
  PasswordResetRequest,
  RegistrationRequest,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  login(request: LoginRequest): Observable<ApiResponse<AuthenticationData>> {
    return this.http.post<ApiResponse<AuthenticationData>>(`${this.baseUrl}/user/login`, request, {
      withCredentials: true,
    });
  }

  verifyMfa(email: string, code: string): Observable<ApiResponse<AuthenticationData>> {
    return this.http.post<ApiResponse<AuthenticationData>>(
      `${this.baseUrl}/user/verify-code`,
      { email, code },
      { withCredentials: true },
    );
  }

  register(
    request: RegistrationRequest,
  ): Observable<ApiResponse<{ user: AuthenticationData['user'] }>> {
    return this.http.post<ApiResponse<{ user: AuthenticationData['user'] }>>(
      `${this.baseUrl}/user/register`,
      request,
    );
  }

  verifyAccount(key: string): Observable<ApiResponse<{ alreadyVerified?: boolean }>> {
    return this.http.get<ApiResponse<{ alreadyVerified?: boolean }>>(
      `${this.baseUrl}/user/verify/account/${encodeURIComponent(key)}`,
    );
  }

  requestPasswordReset(email: string): Observable<ApiResponse> {
    return this.http.get<ApiResponse>(
      `${this.baseUrl}/user/reset-password/${encodeURIComponent(email)}`,
    );
  }

  verifyResetToken(token: string): Observable<ApiResponse<{ valid: boolean }>> {
    return this.http.get<ApiResponse<{ valid: boolean }>>(
      `${this.baseUrl}/user/verify/password/${encodeURIComponent(token)}`,
    );
  }

  resetPassword(request: PasswordResetRequest): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.baseUrl}/user/reset-password`, request);
  }

  refresh(): Observable<ApiResponse<{ accessToken: string }>> {
    return this.http.post<ApiResponse<{ accessToken: string }>>(
      `${this.baseUrl}/user/refresh-token`,
      {},
      { withCredentials: true },
    );
  }
}
