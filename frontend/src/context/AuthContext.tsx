'use client';

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from 'react';
import {
  AuthUser,
  AuthStatus,
  LoginPayload,
  RegisterPayload,
  AuthResponse,
} from '@/types/auth.types';
import { authService } from '@/services/auth.service';
import { tokenStore } from '@/services/token-store';

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<AuthResponse>;
  register: (payload: RegisterPayload) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');

  /**
   * Attempt silent session restoration on app load using the HttpOnly cookie.
   */
  const refreshSession = useCallback(async (): Promise<boolean> => {
    try {
      const response = await authService.refresh();
      if (response && response.user) {
        setUser(response.user);
        setStatus('authenticated');
        return true;
      }
      setUser(null);
      tokenStore.clearAccessToken();
      setStatus('unauthenticated');
      return false;
    } catch {
      // 401 or failed refresh is expected on fresh visits where no cookie exists
      setUser(null);
      tokenStore.clearAccessToken();
      setStatus('unauthenticated');
      return false;
    }
  }, []);

  // Silent restore on mount
  useEffect(() => {
    let isMounted = true;

    async function initSession() {
      await refreshSession();
      if (!isMounted) {
        return;
      }
    }

    initSession();

    return () => {
      isMounted = false;
    };
  }, [refreshSession]);

  const login = async (payload: LoginPayload): Promise<AuthResponse> => {
    setStatus('loading');
    try {
      const response = await authService.login(payload);
      setUser(response.user);
      setStatus('authenticated');
      return response;
    } catch (err) {
      setUser(null);
      tokenStore.clearAccessToken();
      setStatus('unauthenticated');
      throw err;
    }
  };

  const register = async (payload: RegisterPayload): Promise<AuthResponse> => {
    setStatus('loading');
    try {
      const response = await authService.register(payload);
      setUser(response.user);
      setStatus('authenticated');
      return response;
    } catch (err) {
      setUser(null);
      tokenStore.clearAccessToken();
      setStatus('unauthenticated');
      throw err;
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
      tokenStore.clearAccessToken();
      setStatus('unauthenticated');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        status,
        isAuthenticated: status === 'authenticated' && user !== null,
        isLoading: status === 'loading',
        login,
        register,
        logout,
        refreshSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
