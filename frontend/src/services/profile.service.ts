import { API_CONFIG } from '@/config/api.config';
import { apiClient, ApiError } from './api-client';
import {
  CreateProfilePayload,
  DeveloperProfile,
  DeveloperSearchQuery,
  DeveloperSummary,
  PagedResponse,
  UpdateProfilePayload,
} from '@/types/profile.types';

export const profileService = {
  /**
   * Search and filter public developer profiles with pagination.
   */
  async searchDevelopers(
    query?: DeveloperSearchQuery
  ): Promise<PagedResponse<DeveloperSummary>> {
    const params = new URLSearchParams();
    if (query?.keyword && query.keyword.trim()) {
      params.append('keyword', query.keyword.trim());
    }
    if (query?.skill && query.skill.trim()) {
      params.append('skill', query.skill.trim());
    }
    if (query?.experienceLevel) {
      params.append('experienceLevel', query.experienceLevel);
    }
    if (query?.availability) {
      params.append('availability', query.availability);
    }
    if (query?.location && query.location.trim()) {
      params.append('location', query.location.trim());
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
    const endpoint = `${API_CONFIG.endpoints.profiles.base}${qs ? `?${qs}` : ''}`;

    return apiClient<PagedResponse<DeveloperSummary>>(endpoint, {
      skipAuth: true,
    });
  },

  /**
   * Create a new developer profile for the currently authenticated user.
   */
  async createProfile(payload: CreateProfilePayload): Promise<DeveloperProfile> {
    return apiClient<DeveloperProfile>(API_CONFIG.endpoints.profiles.base, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Retrieve the developer profile of the currently authenticated user.
   * Returns null if the user has not yet created a profile (HTTP 404).
   */
  async getMyProfile(): Promise<DeveloperProfile | null> {
    try {
      return await apiClient<DeveloperProfile>(API_CONFIG.endpoints.profiles.me);
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  },

  /**
   * Update the developer profile of the currently authenticated user.
   */
  async updateMyProfile(payload: UpdateProfilePayload): Promise<DeveloperProfile> {
    return apiClient<DeveloperProfile>(API_CONFIG.endpoints.profiles.me, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Upload an avatar image file (JPEG or PNG, up to 5 MB) for the authenticated developer profile.
   */
  async uploadAvatar(file: File): Promise<DeveloperProfile> {
    const formData = new FormData();
    formData.append('file', file);

    return apiClient<DeveloperProfile>(API_CONFIG.endpoints.profiles.avatar, {
      method: 'POST',
      body: formData,
    });
  },

  /**
   * Remove the avatar image from the authenticated developer profile.
   */
  async deleteAvatar(): Promise<DeveloperProfile> {
    return apiClient<DeveloperProfile>(API_CONFIG.endpoints.profiles.avatar, {
      method: 'DELETE',
    });
  },

  /**
   * Retrieve a developer's public profile by their unique username.
   * Returns null if not found (HTTP 404).
   */
  async getPublicProfile(username: string): Promise<DeveloperProfile | null> {
    try {
      return await apiClient<DeveloperProfile>(
        API_CONFIG.endpoints.profiles.byUsername(username),
        { skipAuth: true }
      );
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  },
};

