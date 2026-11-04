/**
 * System Health Type Definitions
 */

export type ServiceStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'CHECKING' | 'DISCONNECTED';
export type DatabaseStatus = 'CONNECTED' | 'DISCONNECTED' | 'DEGRADED' | 'CHECKING';

export interface BackendHealthResponse {
  application: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  database: 'CONNECTED' | 'DISCONNECTED' | 'DEGRADED';
  timestamp?: string;
  environment?: string;
}

export interface SystemHealthState {
  frontend: {
    status: ServiceStatus;
    label: string;
  };
  backend: {
    status: ServiceStatus;
    label: string;
    details?: BackendHealthResponse;
    error?: string;
  };
  database: {
    status: DatabaseStatus;
    label: string;
    error?: string;
  };
  lastChecked: Date | null;
  isLoading: boolean;
}
