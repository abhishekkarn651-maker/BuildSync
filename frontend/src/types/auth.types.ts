/**
 * BuildSync Authentication TypeScript Types
 * Mirrors the Spring Boot backend authentication contracts
 */

export interface AuthUser {
  id: string;
  fullName: string;
  username: string;
  email: string;
  roles: string[];
  emailVerified: boolean;
  avatarUrl?: string | null;
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface RegisterPayload {
  fullName: string;
  username: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface MessageResponse {
  message: string;
}

export interface AuthErrorPayload {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

