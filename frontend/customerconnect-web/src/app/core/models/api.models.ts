export interface UserSummary {
  id?: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  title?: string;
  imageUrl?: string;
  enabled?: boolean;
  usingMfa?: boolean;
}

export interface ApiResponse<T = unknown> {
  timestamp?: string;
  statusCode: number;
  status?: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string[]>;
  data?: T;
}

export interface AuthenticationData {
  user: UserSummary;
  accessToken?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegistrationRequest extends LoginRequest {
  firstName: string;
  lastName: string;
}

export interface PasswordResetRequest {
  token: string;
  password: string;
  confirmPassword: string;
}
