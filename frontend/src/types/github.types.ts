/**
 * GitHub Integration Types
 */

export interface GitHubConnectResponse {
  authorizationUrl: string;
}

export type GitHubTokenStatus = 'VALID' | 'EXPIRED' | 'REVOKED' | 'DISCONNECTED';

export interface GitHubConnectionStatusResponse {
  connected: boolean;
  githubUsername?: string | null;
  githubAvatarUrl?: string | null;
  githubProfileUrl?: string | null;
  repositoryCount: number;
  lastSyncedAt?: string | null;
  tokenStatus: GitHubTokenStatus;
}

export interface GitHubSyncResponse {
  totalCount: number;
  importedCount: number;
  updatedCount: number;
  syncedAt: string;
}

export type RepositoryVisibility = 'PUBLIC' | 'PRIVATE' | 'INTERNAL';

export interface GitHubRepositoryResponse {
  id: string;
  githubRepoId: number;
  name: string;
  fullName: string;
  description: string | null;
  htmlUrl: string;
  isPrivate: boolean;
  visibility: RepositoryVisibility;
  defaultBranch: string | null;
  primaryLanguage: string | null;
  starsCount: number;
  forksCount: number;
  openIssuesCount: number;
  isShowcased: boolean;
  githubPushedAt: string | null;
  githubUpdatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectGitHubRepositoryResponse {
  id: string;
  projectId: string;
  repository: GitHubRepositoryResponse;
  linkedAt: string;
  isShowcased: boolean;
}

export interface PaginatedGitHubRepositories {
  content: GitHubRepositoryResponse[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

