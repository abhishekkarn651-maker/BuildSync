import React from 'react';
import { ServiceStatus, DatabaseStatus } from '@/types/health.types';

interface StatusBadgeProps {
  status: ServiceStatus | DatabaseStatus;
  label?: string;
}

export function StatusBadge({ status, label }: StatusBadgeProps) {
  let badgeStyles = 'bg-slate-800/80 text-slate-300 border-slate-700';
  let dotStyles = 'bg-slate-400';
  let pulseAnimation = false;

  switch (status) {
    case 'UP':
    case 'CONNECTED':
      badgeStyles = 'bg-emerald-950/60 text-emerald-300 border-emerald-500/30';
      dotStyles = 'bg-emerald-400';
      pulseAnimation = true;
      break;
    case 'CHECKING':
      badgeStyles = 'bg-amber-950/60 text-amber-300 border-amber-500/30';
      dotStyles = 'bg-amber-400';
      pulseAnimation = true;
      break;
    case 'DEGRADED':
      badgeStyles = 'bg-orange-950/60 text-orange-300 border-orange-500/30';
      dotStyles = 'bg-orange-400';
      pulseAnimation = false;
      break;
    case 'DOWN':
    case 'DISCONNECTED':
      badgeStyles = 'bg-rose-950/60 text-rose-300 border-rose-500/30';
      dotStyles = 'bg-rose-400';
      pulseAnimation = false;
      break;
  }

  return (
    <span
      className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium border transition-colors ${badgeStyles}`}
    >
      <span className="relative flex h-2 w-2">
        {pulseAnimation && (
          <span
            className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${dotStyles}`}
          />
        )}
        <span className={`relative inline-flex rounded-full h-2 w-2 ${dotStyles}`} />
      </span>
      {label || status}
    </span>
  );
}
