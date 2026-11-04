import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import { tokenStore } from './token-store';
import {
  AuthResponse,
  AuthUser,
  LoginPayload,
  MessageResponse,
  RegisterPayload,
} from '@/types/auth.types';

export const authService = {
  /**
   * Register a new developer account.
   * On success, automatically persists access token in memory and sets HttpOnly cookie.
   */
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await apiClient<AuthResponse>(
      API_CONFIG.endpoints.auth.register,
      {
        method: 'POST',
        body: JSON.stringify(payload),
        skipAuth: true,
      }
    );

    if (response?.accessToken) {
      tokenStore.setAccessToken(response.accessToken);
    }

    return response;
  },

  /**
   * Authenticate user credentials.
   * On success, stores access token in memory and sets HttpOnly cookie.
   */
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await apiClient<AuthResponse>(
      API_CONFIG.endpoints.auth.login,
      {
        method: 'POST',
        body: JSON.stringify(payload),
        skipAuth: true,
      }
    );

    if (response?.accessToken) {
      tokenStore.setAccessToken(response.accessToken);
    }

    return response;
  },

  /**
   * Silent session refresh using HttpOnly cookie.
   */
  async refresh(): Promise<AuthResponse> {
    const response = await apiClient<AuthResponse>(
      API_CONFIG.endpoints.auth.refresh,
      {
        method: 'POST',
        skipAuth: true,
        skipRefreshRetry: true,
      }
    );

    if (response?.accessToken) {
      tokenStore.setAccessToken(response.accessToken);
    }

    return response;
  },

  /**
   * Securely terminate authenticated session.
   * Cleans up server-side refresh token and in-memory access token.
   */
  async logout(): Promise<MessageResponse> {
    try {
      return await apiClient<MessageResponse>(
        API_CONFIG.endpoints.auth.logout,
        {
          method: 'POST',
          skipRefreshRetry: true,
        }
      );
    } catch {
      // In case server is unreachable, we still ensure local state is cleared
      return { message: 'Logged out locally' };
    } finally {
      tokenStore.clearAccessToken();
    }
  },

  /**
   * Retrieve the currently authenticated user's profile and roles.
   */
  async getCurrentUser(): Promise<AuthUser> {
    return apiClient<AuthUser>(API_CONFIG.endpoints.auth.me);
  },
};

