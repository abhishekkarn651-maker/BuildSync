import { API_CONFIG } from '@/config/api.config';
import { apiClient, ApiError } from './api-client';
import { PagedResponse } from '@/types/profile.types';
import {
  CreateProjectPayload,
  Project,
  ProjectSearchQuery,
  ProjectSummary,
  ProjectTechnology,
  UpdateProjectPayload,
} from '@/types/project.types';

export const projectService = {
  /**
   * Create a new project owned by the authenticated developer.
   */
  async createProject(payload: CreateProjectPayload): Promise<Project> {
    return apiClient<Project>(API_CONFIG.endpoints.projects.base, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Retrieve all projects owned by the currently authenticated developer.
   */
  async getMyProjects(): Promise<ProjectSummary[]> {
    return apiClient<ProjectSummary[]>(API_CONFIG.endpoints.projects.mine);
  },

  /**
   * Retrieve project details by ID or unique slug.
   * Public projects are accessible without authentication.
   * Private projects return null if not authorized.
   */
  async getProject(idOrSlug: string): Promise<Project | null> {
    try {
      return await apiClient<Project>(API_CONFIG.endpoints.projects.byId(idOrSlug));
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  },

  /**
   * Update an existing project. Only the project owner can update their project.
   */
  async updateProject(
    id: string,
    payload: UpdateProjectPayload
  ): Promise<Project> {
    return apiClient<Project>(API_CONFIG.endpoints.projects.byId(id), {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Delete an existing project. Only the project owner can delete their project.
   */
  async deleteProject(id: string): Promise<void> {
    await apiClient<void>(API_CONFIG.endpoints.projects.byId(id), {
      method: 'DELETE',
    });
  },

  /**
   * Browse and search public projects with dynamic multi-criteria filters and pagination.
   */
  async browseProjects(
    query?: ProjectSearchQuery
  ): Promise<PagedResponse<ProjectSummary>> {
    const params = new URLSearchParams();
    if (query?.keyword && query.keyword.trim()) {
      params.append('keyword', query.keyword.trim());
    }
    if (query?.category) {
      params.append('category', query.category);
    }
    if (query?.status) {
      params.append('status', query.status);
    }
    if (query?.technology && query.technology.trim()) {
      params.append('technology', query.technology.trim());
    }
    if (typeof query?.page === 'number') {
      params.append('page', query.page.toString());
    }
    if (typeof query?.size === 'number') {
      params.append('size', query.size.toString());
    }
    if (query?.sort) {
      params.append('sort', query.sort);
    }

    const qs = params.toString();
    const endpoint = `${API_CONFIG.endpoints.projects.base}${qs ? `?${qs}` : ''}`;

    return apiClient<PagedResponse<ProjectSummary>>(endpoint, {
      skipAuth: true,
    });
  },

  /**
   * Retrieve technologies associated with a project.
   */
  async getProjectTechnologies(idOrSlug: string): Promise<ProjectTechnology[]> {
    return apiClient<ProjectTechnology[]>(
      API_CONFIG.endpoints.projects.technologies(idOrSlug),
      { skipAuth: true }
    );
  },
};

