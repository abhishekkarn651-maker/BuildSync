import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import { Education, EducationPayload } from '@/types/profile.types';

export const educationService = {
  /**
   * Retrieve all education records for the authenticated user.
   */
  async getMyEducation(): Promise<Education[]> {
    return apiClient<Education[]>(API_CONFIG.endpoints.education.me);
  },

  /**
   * Add a new education entry to the authenticated user's profile.
   */
  async addEducation(payload: EducationPayload): Promise<Education> {
    return apiClient<Education>(API_CONFIG.endpoints.education.me, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Update an existing education entry.
   */
  async updateEducation(id: string, payload: EducationPayload): Promise<Education> {
    return apiClient<Education>(API_CONFIG.endpoints.education.meById(id), {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Delete an education entry from the profile.
   */
  async deleteEducation(id: string): Promise<void> {
    await apiClient<void>(API_CONFIG.endpoints.education.meById(id), {
      method: 'DELETE',
    });
  },
};

