import { API_CONFIG } from '@/config/api.config';
import { tokenStore } from './token-store';
import { AuthResponse } from '@/types/auth.types';

export interface ApiClientOptions extends RequestInit {
  skipAuth?: boolean;
  skipRefreshRetry?: boolean;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
    public data?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class RateLimitError extends ApiError {
  constructor(
    message: string,
    public retryAfter?: number,
    public rateLimitRemaining?: number,
    public rateLimitReset?: number,
    status: number = 429,
    data?: unknown
  ) {
    super(message, status, data);
    this.name = 'RateLimitError';
  }
}

// Single-flight shared refresh promise for deduplication
let activeRefreshPromise: Promise<AuthResponse | null> | null = null;

/**
 * Deduplicated token refresher to avoid concurrent token rotation collisions.
 * Both AuthContext session initialization and apiClient 401 retry share this single flight.
 */
export async function performSilentRefresh(): Promise<AuthResponse | null> {
  if (activeRefreshPromise) {
    return activeRefreshPromise;
  }

  activeRefreshPromise = (async () => {
    try {
      const cleanBase = API_CONFIG.baseUrl.replace(/\/+$/, '');
      const refreshPath = API_CONFIG.endpoints.auth.refresh.startsWith('/')
        ? API_CONFIG.endpoints.auth.refresh
        : `/${API_CONFIG.endpoints.auth.refresh}`;
      const response = await fetch(`${cleanBase}${refreshPath}`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        tokenStore.clearAccessToken();
        return null;
      }

      const data = (await response.json()) as AuthResponse;
      if (data && data.accessToken) {
        tokenStore.setAccessToken(data.accessToken);
        return data;
      }

      tokenStore.clearAccessToken();
      return null;
    } catch {
      tokenStore.clearAccessToken();
      return null;
    } finally {
      activeRefreshPromise = null;
    }
  })();

  return activeRefreshPromise;
}

export async function apiClient<T>(
  endpoint: string,
  options: ApiClientOptions = {}
): Promise<T> {
  const cleanBase = API_CONFIG.baseUrl.replace(/\/+$/, '');
  const cleanPath = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${cleanBase}${cleanPath}`;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), API_CONFIG.timeoutMs);

  const isAuthEndpoint =
    endpoint.includes('/api/v1/auth/login') ||
    endpoint.includes('/api/v1/auth/register') ||
    endpoint.includes('/api/v1/auth/refresh') ||
    endpoint.includes('/api/v1/auth/logout');

  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;

  const headers: Record<string, string> = {
    'Accept': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };

  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  // Inject in-memory access token unless explicitly skipped
  if (!options.skipAuth) {
    const token = tokenStore.getAccessToken();
    if (token && !headers['Authorization']) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      credentials: 'include', // Ensure HttpOnly cookies are transmitted
      signal: controller.signal,
      headers,
    });

    clearTimeout(timeoutId);

    // Automatic 401 retry with silent token refresh for protected resources (at most once)
    if (response.status === 401 && !isAuthEndpoint && !options.skipRefreshRetry) {
      const refreshedAuth = await performSilentRefresh();
      if (refreshedAuth && refreshedAuth.accessToken) {
        // Retry original request at most once with newly refreshed access token
        return apiClient<T>(endpoint, {
          ...options,
          skipRefreshRetry: true,
          headers: {
            ...headers,
            'Authorization': `Bearer ${refreshedAuth.accessToken}`,
          },
        });
      }
    }

    if (!response.ok) {
      if (response.status === 429) {
        const retryAfterHeader = response.headers.get('Retry-After');
        const remainingHeader = response.headers.get('RateLimit-Remaining');
        const resetHeader = response.headers.get('RateLimit-Reset');

        const retryAfter = retryAfterHeader ? parseInt(retryAfterHeader, 10) : undefined;
        const rateLimitRemaining = remainingHeader ? parseInt(remainingHeader, 10) : undefined;
        const rateLimitReset = resetHeader ? parseInt(resetHeader, 10) : undefined;

        let errorBody: unknown;
        try {
          errorBody = await response.json();
        } catch {
          errorBody = await response.text();
        }

        let errorMessage = 'Too many requests. Please try again later.';
        if (typeof errorBody === 'object' && errorBody !== null && 'message' in errorBody) {
          errorMessage = String((errorBody as { message: unknown }).message);
        } else if (typeof errorBody === 'string' && errorBody) {
          errorMessage = errorBody;
        }

        throw new RateLimitError(
          errorMessage,
          retryAfter,
          rateLimitRemaining,
          rateLimitReset,
          429,
          errorBody
        );
      }

      let errorBody: unknown;
      try {
        errorBody = await response.json();
      } catch {
        errorBody = await response.text();
      }

      let errorMessage: string | undefined;
      if (typeof errorBody === 'object' && errorBody !== null && 'message' in errorBody) {
        errorMessage = String((errorBody as { message: unknown }).message);
      } else if (typeof errorBody === 'string' && errorBody) {
        errorMessage = errorBody;
      }

      throw new ApiError(
        errorMessage || `API request failed with status ${response.status}`,
        response.status,
        errorBody
      );
    }

    return (await response.json()) as T;
  } catch (error: unknown) {
    clearTimeout(timeoutId);

    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('Request timed out while connecting to the backend server', 408);
    }

    if (error instanceof TypeError && error.message.includes('fetch')) {
      const hint = API_CONFIG.baseUrl.includes('localhost')
        ? ' Is the backend running on port 8080?'
        : ' Please check your network connection and backend service status.';
      throw new ApiError(`Unable to connect to the backend server.${hint}`, 503);
    }

    const message = error instanceof Error ? error.message : 'An unexpected network error occurred';
    throw new ApiError(message, 500);
  }
}
