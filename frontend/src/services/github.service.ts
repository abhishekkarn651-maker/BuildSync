import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import {
  GitHubConnectResponse,
  GitHubConnectionStatusResponse,
  GitHubRepositoryResponse,
  GitHubSyncResponse,
  PaginatedGitHubRepositories,
  ProjectGitHubRepositoryResponse,
} from '@/types/github.types';

export const githubService = {
  /**
   * Initiates GitHub PKCE OAuth connection.
   */
  async connect(): Promise<GitHubConnectResponse> {
    return apiClient<GitHubConnectResponse>(API_CONFIG.endpoints.github.connect, {
      method: 'POST',
    });
  },

  /**
   * Fetches current GitHub connection status.
   */
  async getStatus(): Promise<GitHubConnectionStatusResponse> {
    return apiClient<GitHubConnectionStatusResponse>(API_CONFIG.endpoints.github.status);
  },

  /**
   * Disconnects GitHub account and revokes upstream token.
   */
  async disconnect(): Promise<void> {
    return apiClient<void>(API_CONFIG.endpoints.github.disconnect, {
      method: 'DELETE',
    });
  },

  /**
   * Synchronizes repositories from GitHub into BuildSync.
   */
  async syncRepositories(): Promise<GitHubSyncResponse> {
    return apiClient<GitHubSyncResponse>(API_CONFIG.endpoints.github.sync, {
      method: 'POST',
    });
  },

  /**
   * Lists imported repositories with filtering and pagination.
   */
  async getRepositories(
    page = 0,
    size = 12,
    search?: string,
    language?: string,
    isPrivate?: boolean,
    isShowcased?: boolean,
    sortBy = 'pushedAt'
  ): Promise<PaginatedGitHubRepositories> {
    return apiClient<PaginatedGitHubRepositories>(
      API_CONFIG.endpoints.github.repositories(
        page,
        size,
        search,
        language,
        isPrivate,
        isShowcased,
        sortBy
      )
    );
  },

  /**
   * Fetches a single repository by ID.
   */
  async getRepositoryById(repositoryId: string): Promise<GitHubRepositoryResponse> {
    return apiClient<GitHubRepositoryResponse>(
      API_CONFIG.endpoints.github.repositoryById(repositoryId)
    );
  },

  /**
   * Toggles whether an imported repository is showcased on the public developer profile.
   */
  async toggleShowcase(
    repositoryId: string,
    isShowcased: boolean
  ): Promise<GitHubRepositoryResponse> {
    return apiClient<GitHubRepositoryResponse>(
      API_CONFIG.endpoints.github.toggleShowcase(repositoryId),
      {
        method: 'PATCH',
        body: JSON.stringify({ isShowcased }),
      }
    );
  },

  /**
   * Fetches public showcased repositories for a developer username.
   */
  async getPublicShowcase(username: string): Promise<GitHubRepositoryResponse[]> {
    return apiClient<GitHubRepositoryResponse[]>(
      API_CONFIG.endpoints.github.publicShowcase(username)
    );
  },

  /**
   * Fetches repositories linked to a specific BuildSync project.
   */
  async getProjectRepositories(projectId: string): Promise<ProjectGitHubRepositoryResponse[]> {
    return apiClient<ProjectGitHubRepositoryResponse[]>(
      API_CONFIG.endpoints.github.projectRepositories(projectId)
    );
  },

  /**
   * Links an imported repository to a BuildSync project.
   */
  async linkProjectRepository(
    projectId: string,
    githubRepositoryId: string,
    isShowcased = true
  ): Promise<ProjectGitHubRepositoryResponse> {
    return apiClient<ProjectGitHubRepositoryResponse>(
      API_CONFIG.endpoints.github.linkProjectRepository(projectId),
      {
        method: 'POST',
        body: JSON.stringify({ githubRepositoryId, isShowcased }),
      }
    );
  },

  /**
   * Unlinks an imported repository from a BuildSync project.
   */
  async unlinkProjectRepository(projectId: string, repositoryId: string): Promise<void> {
    return apiClient<void>(
      API_CONFIG.endpoints.github.unlinkProjectRepository(projectId, repositoryId),
      {
        method: 'DELETE',
      }
    );
  },
};

