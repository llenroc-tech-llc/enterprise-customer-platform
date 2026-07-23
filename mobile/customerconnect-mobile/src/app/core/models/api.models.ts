export interface UserSummary {
  id?: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  title?: string;
  usingMfa?: boolean;
}
export interface ApiResponse<T = unknown> {
  statusCode: number;
  message: string;
  data?: T;
  validationErrors?: Record<string, string[]>;
}
export interface AuthenticationData {
  user: UserSummary;
  accessToken?: string;
}
export interface StoredSession {
  user: UserSummary;
  accessToken: string;
}
