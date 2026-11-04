import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import {
  AddSkillPayload,
  ProfileSkill,
  Skill,
  SkillCategory,
  UpdateSkillProficiencyPayload,
} from '@/types/profile.types';

export const skillsService = {
  /**
   * Search master skills catalogue by keyword and category (Public).
   */
  async searchCatalogue(search?: string, category?: SkillCategory): Promise<Skill[]> {
    return apiClient<Skill[]>(API_CONFIG.endpoints.skills.catalogue(search, category), {
      skipAuth: true,
    });
  },

  /**
   * Retrieve all skills assigned to current authenticated developer.
   */
  async getMySkills(): Promise<ProfileSkill[]> {
    return apiClient<ProfileSkill[]>(API_CONFIG.endpoints.skills.me);
  },

  /**
   * Assign a skill with proficiency level to the authenticated developer's profile.
   */
  async addSkill(payload: AddSkillPayload): Promise<ProfileSkill> {
    return apiClient<ProfileSkill>(API_CONFIG.endpoints.skills.me, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Update the proficiency level of an assigned skill.
   */
  async updateProficiency(
    skillId: string,
    payload: UpdateSkillProficiencyPayload
  ): Promise<ProfileSkill> {
    return apiClient<ProfileSkill>(API_CONFIG.endpoints.skills.meById(skillId), {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Remove an assigned skill from the developer profile.
   */
  async removeSkill(skillId: string): Promise<void> {
    await apiClient<void>(API_CONFIG.endpoints.skills.meById(skillId), {
      method: 'DELETE',
    });
  },
};

