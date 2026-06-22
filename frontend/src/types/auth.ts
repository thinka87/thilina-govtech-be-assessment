export type Role = 'ADMIN' | 'SERVICE_AGENT' | 'CITIZEN';

export interface AuthUser {
  username: string;
  role: Role;
  mustChangePassword: boolean;
  citizenReference?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: Role;
  mustChangePassword: boolean;
  citizenReference?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
