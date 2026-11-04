'use client';

import React from 'react';
import { Server, Database, RefreshCw, CheckCircle2, AlertCircle, FileCode2, ExternalLink } from 'lucide-react';
import { useSystemHealth } from '@/hooks/useSystemHealth';
import { API_CONFIG } from '@/config/api.config';

export function SystemHealthWidget() {
  const { health, refresh } = useSystemHealth(15000);

  const isBackendUp = health.backend.status === 'UP';
  const isDbConnected = health.database.status === 'CONNECTED';

  return (
    <div className="rounded-xl bg-card border border-border p-5 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border pb-3">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <h2 className="text-sm sm:text-base font-bold text-foreground tracking-tight">
            System Infrastructure
          </h2>
        </div>
        <button
          type="button"
          onClick={() => refresh()}
          disabled={health.isLoading}
          className="p-1 rounded text-muted hover:text-foreground transition-colors disabled:opacity-50"
          title="Refresh health checks"
          aria-label="Refresh system health"
        >
          <RefreshCw
            className={`w-3.5 h-3.5 ${health.isLoading ? 'animate-spin text-[#EF4444]' : ''}`}
          />
        </button>
      </div>

      {/* Grid of Backend & Database connectivity */}
      <div className="grid grid-cols-2 gap-2.5">
        {/* Backend status */}
        <div className="p-3 rounded-lg bg-surface border border-border space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-muted font-medium flex items-center gap-1.5">
              <Server className="w-3.5 h-3.5 text-muted" />
              Spring Boot
            </span>
            {isBackendUp ? (
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            ) : (
              <AlertCircle className="w-3.5 h-3.5 text-red-500" />
            )}
          </div>
          <div className="text-xs font-bold text-foreground">
            {isBackendUp ? 'ONLINE (UP)' : 'OFFLINE'}
          </div>
          <div className="text-[10px] text-muted">
            {API_CONFIG.baseUrl.includes(':8080') ? 'Port 8080 / REST API' : 'Backend REST API'}
          </div>
        </div>

        {/* PostgreSQL status */}
        <div className="p-3 rounded-lg bg-surface border border-border space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-muted font-medium flex items-center gap-1.5">
              <Database className="w-3.5 h-3.5 text-muted" />
              PostgreSQL 17
            </span>
            {isDbConnected ? (
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            ) : (
              <AlertCircle className="w-3.5 h-3.5 text-red-500" />
            )}
          </div>
          <div className="text-xs font-bold text-foreground">
            {isDbConnected ? 'CONNECTED' : 'DISCONNECTED'}
          </div>
          <div className="text-[10px] text-muted">buildsync_db / Flyway v1</div>
        </div>
      </div>

      {/* Documentation link */}
      <div className="pt-1">
        <a
          href={`${API_CONFIG.baseUrl}${API_CONFIG.endpoints.swagger}`}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center justify-between w-full p-2.5 rounded-lg bg-surface hover:bg-surface-hover border border-border text-xs text-muted hover:text-foreground transition-colors group"
        >
          <div className="flex items-center gap-2">
            <FileCode2 className="w-4 h-4 text-[#EF4444]" />
            <span className="font-medium">Backend OpenAPI Swagger UI</span>
          </div>
          <ExternalLink className="w-3.5 h-3.5 text-muted group-hover:text-foreground" />
        </a>
      </div>
    </div>
  );
}

