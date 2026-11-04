import { apiClient } from './api-client';
import { API_CONFIG } from '@/config/api.config';
import { BackendHealthResponse } from '@/types/health.types';

export const HealthService = {
  /**
   * Fetches backend and database health status
   */
  async checkHealth(): Promise<BackendHealthResponse> {
    return apiClient<BackendHealthResponse>(API_CONFIG.endpoints.health, {
      method: 'GET',
      cache: 'no-store',
    });
  },
};
