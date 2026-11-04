/**
 * BuildSync API Configuration
 * Centralized endpoint definitions and environment configurations
 */

/**
 * Resolves the backend REST API base URL.
 * Supports NEXT_PUBLIC_API_URL (recommended) and NEXT_PUBLIC_API_BASE_URL (backward-compatible).
 * Normalizes by trimming and stripping trailing slashes to prevent double slashes.
 * In development, falls back to 'http://localhost:8080'.
 * In production, prevents silent localhost fallback.
 */
export function resolveApiBaseUrl(): string {
  const envUrl = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_API_BASE_URL;

  if (envUrl && envUrl.trim()) {
    return envUrl.trim().replace(/\/+$/, '');
  }

  // In production, do not silently send client traffic to localhost
  if (process.env.NODE_ENV === 'production') {
    if (typeof window !== 'undefined') {
      console.warn(
        '[BuildSync API Config] NEXT_PUBLIC_API_URL is not set in production. ' +
        'Please configure NEXT_PUBLIC_API_URL in your Vercel Project Settings > Environment Variables.'
      );
      // Fallback to origin rather than user's localhost if running in browser
      return window.location.origin.replace(/\/+$/, '');
    }
  }

  // Development default
  return 'http://localhost:8080';
}

export const API_CONFIG = {
  baseUrl: resolveApiBaseUrl(),
  timeoutMs: 8000,
  endpoints: {
    health: '/api/v1/health',
    swagger: '/swagger-ui/index.html',
    apiDocs: '/v3/api-docs',
    auth: {
      register: '/api/v1/auth/register',
      login: '/api/v1/auth/login',
      refresh: '/api/v1/auth/refresh',
      logout: '/api/v1/auth/logout',
      me: '/api/v1/auth/me',
    },
    profiles: {
      base: '/api/v1/profiles',
      me: '/api/v1/profiles/me',
      avatar: '/api/v1/profiles/me/avatar',
      byUsername: (username: string) => `/api/v1/profiles/${encodeURIComponent(username)}`,
    },
    skills: {
      catalogue: (search?: string, category?: string) => {
        const params = new URLSearchParams();
        if (search && search.trim()) params.append('search', search.trim());
        if (category && category.trim()) params.append('category', category.trim());
        const qs = params.toString();
        return `/api/v1/skills${qs ? `?${qs}` : ''}`;
      },
      me: '/api/v1/profiles/me/skills',
      meById: (skillId: string) => `/api/v1/profiles/me/skills/${encodeURIComponent(skillId)}`,
    },
    education: {
      me: '/api/v1/profiles/me/education',
      meById: (id: string) => `/api/v1/profiles/me/education/${encodeURIComponent(id)}`,
    },
    experience: {
      me: '/api/v1/profiles/me/experience',
      meById: (id: string) => `/api/v1/profiles/me/experience/${encodeURIComponent(id)}`,
    },
    projects: {
      base: '/api/v1/projects',
      mine: '/api/v1/projects/mine',
      byId: (idOrSlug: string) => `/api/v1/projects/${encodeURIComponent(idOrSlug)}`,
      technologies: (idOrSlug: string) => `/api/v1/projects/${encodeURIComponent(idOrSlug)}/technologies`,
      joinRequests: (projectId: string) => `/api/v1/projects/${encodeURIComponent(projectId)}/join-requests`,
      invitations: (projectId: string) => `/api/v1/projects/${encodeURIComponent(projectId)}/invitations`,
      members: (projectId: string) => `/api/v1/projects/${encodeURIComponent(projectId)}/members`,
      removeMember: (projectId: string, userId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/members/${encodeURIComponent(userId)}`,
      leave: (projectId: string) => `/api/v1/projects/${encodeURIComponent(projectId)}/leave`,
      pendingRequests: (projectId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/pending-requests`,
    },
    collaborations: {
      incoming: (page = 0, size = 10) => `/api/v1/collaborations/incoming?page=${page}&size=${size}`,
      outgoing: (page = 0, size = 10) => `/api/v1/collaborations/outgoing?page=${page}&size=${size}`,
      myTeams: '/api/v1/collaborations/my-teams',
      accept: (requestId: string) => `/api/v1/collaborations/${encodeURIComponent(requestId)}/accept`,
      reject: (requestId: string) => `/api/v1/collaborations/${encodeURIComponent(requestId)}/reject`,
      cancel: (requestId: string) => `/api/v1/collaborations/${encodeURIComponent(requestId)}/cancel`,
    },
    github: {
      connect: '/api/v1/integrations/github/connect',
      status: '/api/v1/integrations/github/status',
      disconnect: '/api/v1/integrations/github/disconnect',
      sync: '/api/v1/integrations/github/sync',
      repositories: (
        page = 0,
        size = 12,
        search?: string,
        language?: string,
        isPrivate?: boolean,
        isShowcased?: boolean,
        sortBy = 'pushedAt'
      ) => {
        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('size', size.toString());
        if (search && search.trim()) params.append('search', search.trim());
        if (language && language.trim()) params.append('language', language.trim());
        if (typeof isPrivate === 'boolean') params.append('isPrivate', isPrivate.toString());
        if (typeof isShowcased === 'boolean') params.append('isShowcased', isShowcased.toString());
        if (sortBy && sortBy.trim()) params.append('sortBy', sortBy.trim());
        return `/api/v1/integrations/github/repositories?${params.toString()}`;
      },
      repositoryById: (repositoryId: string) =>
        `/api/v1/integrations/github/repositories/${encodeURIComponent(repositoryId)}`,
      toggleShowcase: (repositoryId: string) =>
        `/api/v1/integrations/github/repositories/${encodeURIComponent(repositoryId)}/showcase`,
      publicShowcase: (username: string) =>
        `/api/v1/integrations/github/showcase/${encodeURIComponent(username)}`,
      projectRepositories: (projectId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/github-repositories`,
      linkProjectRepository: (projectId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/github-repositories`,
      unlinkProjectRepository: (projectId: string, repositoryId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/github-repositories/${encodeURIComponent(repositoryId)}`,
    },
    conversations: {
      base: (page = 0, size = 20) => `/api/v1/conversations?page=${page}&size=${size}`,
      direct: '/api/v1/conversations/direct',
      byId: (id: string) => `/api/v1/conversations/${encodeURIComponent(id)}`,
      messages: (id: string, page = 0, size = 50) =>
        `/api/v1/conversations/${encodeURIComponent(id)}/messages?page=${page}&size=${size}`,
      send: (id: string) => `/api/v1/conversations/${encodeURIComponent(id)}/messages`,
      read: (id: string) => `/api/v1/conversations/${encodeURIComponent(id)}/read`,
      unreadCount: '/api/v1/conversations/unread-count',
      projectConversation: (projectId: string) =>
        `/api/v1/projects/${encodeURIComponent(projectId)}/conversation`,
    },
  },
} as const;

/**
 * Resolves the WebSocket STOMP endpoint URL.
 * Supports NEXT_PUBLIC_WS_URL or derives from backend API base URL.
 * Automatically switches http -> ws and https -> wss.
 * Enforces wss:// in production and on HTTPS origins.
 * Guarantees no duplicate slashes or malformed endpoints.
 */
export function getWebSocketUrl(): string {
  const envWsUrl = process.env.NEXT_PUBLIC_WS_URL;

  if (envWsUrl && envWsUrl.trim()) {
    let wsUrl = envWsUrl.trim().replace(/\/+$/, '');
    const isHttpsOrigin = typeof window !== 'undefined' && window.location.protocol === 'https:';
    if ((process.env.NODE_ENV === 'production' || isHttpsOrigin) && wsUrl.startsWith('ws://')) {
      wsUrl = wsUrl.replace(/^ws:\/\//, 'wss://');
    }
    return wsUrl.endsWith('/ws') ? wsUrl : `${wsUrl}/ws`;
  }

  const baseUrl = API_CONFIG.baseUrl.replace(/\/+$/, '');
  if (!baseUrl) {
    return '';
  }

  let wsUrl = baseUrl.replace(/^http:/i, 'ws:').replace(/^https:/i, 'wss:');

  const isHttpsOrigin = typeof window !== 'undefined' && window.location.protocol === 'https:';
  if (
    (process.env.NODE_ENV === 'production' || isHttpsOrigin) &&
    wsUrl.startsWith('ws://') &&
    !wsUrl.includes('localhost') &&
    !wsUrl.includes('127.0.0.1')
  ) {
    wsUrl = wsUrl.replace(/^ws:\/\//i, 'wss://');
  }

  return wsUrl.endsWith('/ws') ? wsUrl : `${wsUrl}/ws`;
}

/**
 * Resolves a full public avatar URL from a relative or absolute avatar reference.
 * Normalizes leading and trailing slashes to eliminate duplicate slashes.
 */
export function getAvatarUrl(url?: string | null): string | undefined {
  if (!url || !url.trim()) return undefined;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const baseUrl = API_CONFIG.baseUrl.replace(/\/+$/, '');
  const path = url.startsWith('/') ? url : `/${url}`;
  return `${baseUrl}${path}`;
}
