import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_BASE_URL } from '../configuration/api.config';
import { ApiResponse, AuthenticationData } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class MobileAuthApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  login(email: string, password: string) {
    return this.http.post<ApiResponse<AuthenticationData>>(
      `${this.base}/user/login`,
      { email, password },
      { withCredentials: true },
    );
  }
  verifyMfa(email: string, code: string) {
    return this.http.post<ApiResponse<AuthenticationData>>(
      `${this.base}/user/verify-code`,
      { email, code },
      { withCredentials: true },
    );
  }
  register(request: { firstName: string; lastName: string; email: string; password: string }) {
    return this.http.post<ApiResponse>(`${this.base}/user/register`, request);
  }
  verifyAccount(key: string) {
    return this.http.get<ApiResponse>(
      `${this.base}/user/verify/account/${encodeURIComponent(key)}`,
    );
  }
  forgotPassword(email: string) {
    return this.http.get<ApiResponse>(
      `${this.base}/user/reset-password/${encodeURIComponent(email)}`,
    );
  }
  verifyReset(token: string) {
    return this.http.get<ApiResponse<{ valid: boolean }>>(
      `${this.base}/user/verify/password/${encodeURIComponent(token)}`,
    );
  }
  resetPassword(token: string, password: string, confirmPassword: string) {
    return this.http.post<ApiResponse>(`${this.base}/user/reset-password`, {
      token,
      password,
      confirmPassword,
    });
  }
  refresh() {
    return this.http.post<ApiResponse<{ accessToken: string }>>(
      `${this.base}/user/refresh-token`,
      {},
      { withCredentials: true },
    );
  }
}
