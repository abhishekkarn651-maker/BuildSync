/**
 * BuildSync In-Memory Token Store
 *
 * CRITICAL SECURITY REQUIREMENT:
 * The JWT access token is stored strictly in memory (JavaScript runtime variable).
 * It is NEVER stored in localStorage, sessionStorage, or non-HttpOnly cookies.
 * This completely eliminates cross-site scripting (XSS) token extraction vectors.
 *
 * The long-lived refresh token is persisted exclusively in a secure HttpOnly cookie
 * managed directly by the Spring Boot backend on /api/v1/auth paths.
 */

let inMemoryAccessToken: string | null = null;
type TokenListener = (token: string | null) => void;
const listeners = new Set<TokenListener>();

export const tokenStore = {
  getAccessToken(): string | null {
    return inMemoryAccessToken;
  },

  setAccessToken(token: string | null): void {
    inMemoryAccessToken = token;
    listeners.forEach((listener) => {
      try {
        listener(token);
      } catch (e) {
        console.error('Error in tokenStore listener:', e);
      }
    });
  },

  clearAccessToken(): void {
    inMemoryAccessToken = null;
    listeners.forEach((listener) => {
      try {
        listener(null);
      } catch (e) {
        console.error('Error in tokenStore listener:', e);
      }
    });
  },

  subscribe(listener: TokenListener): () => void {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};

