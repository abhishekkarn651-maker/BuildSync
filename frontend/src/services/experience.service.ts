import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import { ExperiencePayload, WorkExperience } from '@/types/profile.types';

export const experienceService = {
  /**
   * Retrieve all work experience entries for the authenticated user.
   */
  async getMyExperience(): Promise<WorkExperience[]> {
    return apiClient<WorkExperience[]>(API_CONFIG.endpoints.experience.me);
  },

  /**
   * Add a new work experience entry to the authenticated user's profile.
   */
  async addExperience(payload: ExperiencePayload): Promise<WorkExperience> {
    return apiClient<WorkExperience>(API_CONFIG.endpoints.experience.me, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Update an existing work experience entry.
   */
  async updateExperience(id: string, payload: ExperiencePayload): Promise<WorkExperience> {
    return apiClient<WorkExperience>(API_CONFIG.endpoints.experience.meById(id), {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Delete a work experience entry from the profile.
   */
  async deleteExperience(id: string): Promise<void> {
    await apiClient<void>(API_CONFIG.endpoints.experience.meById(id), {
      method: 'DELETE',
    });
  },
};

