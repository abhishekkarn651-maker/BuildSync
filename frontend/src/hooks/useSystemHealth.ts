'use client';

import { useState, useEffect, useCallback } from 'react';
import { HealthService } from '@/services/health.service';
import { SystemHealthState } from '@/types/health.types';
import { ApiError } from '@/services/api-client';

export function useSystemHealth(autoRefreshIntervalMs = 15000) {
  const [health, setHealth] = useState<SystemHealthState>({
    frontend: { status: 'UP', label: 'Running' },
    backend: { status: 'CHECKING', label: 'Checking connection...' },
    database: { status: 'CHECKING', label: 'Checking connection...' },
    lastChecked: null,
    isLoading: true,
  });

  const performHealthCheck = useCallback(async () => {
    try {
      const response = await HealthService.checkHealth();

      setHealth({
        frontend: { status: 'UP', label: 'Running' },
        backend: {
          status: response.status === 'UP' ? 'UP' : 'DEGRADED',
          label: response.status === 'UP' ? 'Connected' : 'Degraded',
          details: response,
        },
        database: {
          status: response.database === 'CONNECTED' ? 'CONNECTED' : 'DISCONNECTED',
          label: response.database === 'CONNECTED' ? 'Connected' : 'Disconnected',
        },
        lastChecked: new Date(),
        isLoading: false,
      });
    } catch (err: unknown) {
      const errorMessage = err instanceof ApiError ? err.message : 'Connection failed';
      setHealth({
        frontend: { status: 'UP', label: 'Running' },
        backend: {
          status: 'DOWN',
          label: 'Disconnected',
          error: errorMessage,
        },
        database: {
          status: 'DISCONNECTED',
          label: 'Unreachable via Backend',
          error: 'Backend is offline',
        },
        lastChecked: new Date(),
        isLoading: false,
      });
    }
  }, []);

  const refresh = useCallback(() => {
    setHealth((prev) => ({
      ...prev,
      isLoading: true,
      backend: prev.backend.status === 'UP' ? prev.backend : { status: 'CHECKING', label: 'Checking connection...' },
      database: prev.database.status === 'CONNECTED' ? prev.database : { status: 'CHECKING', label: 'Checking connection...' },
    }));
    void performHealthCheck();
  }, [performHealthCheck]);

  useEffect(() => {
    let isCancelled = false;

    const runAsyncCheck = async () => {
      try {
        const response = await HealthService.checkHealth();
        if (isCancelled) return;

        setHealth({
          frontend: { status: 'UP', label: 'Running' },
          backend: {
            status: response.status === 'UP' ? 'UP' : 'DEGRADED',
            label: response.status === 'UP' ? 'Connected' : 'Degraded',
            details: response,
          },
          database: {
            status: response.database === 'CONNECTED' ? 'CONNECTED' : 'DISCONNECTED',
            label: response.database === 'CONNECTED' ? 'Connected' : 'Disconnected',
          },
          lastChecked: new Date(),
          isLoading: false,
        });
      } catch (err: unknown) {
        if (isCancelled) return;
        const errorMessage = err instanceof ApiError ? err.message : 'Connection failed';
        setHealth({
          frontend: { status: 'UP', label: 'Running' },
          backend: {
            status: 'DOWN',
            label: 'Disconnected',
            error: errorMessage,
          },
          database: {
            status: 'DISCONNECTED',
            label: 'Unreachable via Backend',
            error: 'Backend is offline',
          },
          lastChecked: new Date(),
          isLoading: false,
        });
      }
    };

    void runAsyncCheck();

    if (autoRefreshIntervalMs > 0) {
      const interval = setInterval(runAsyncCheck, autoRefreshIntervalMs);
      return () => {
        isCancelled = true;
        clearInterval(interval);
      };
    }

    return () => {
      isCancelled = true;
    };
  }, [autoRefreshIntervalMs]);

  return {
    health,
    refresh,
  };
}
