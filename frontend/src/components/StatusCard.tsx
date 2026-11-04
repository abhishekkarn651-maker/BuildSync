import React from 'react';
import { LucideIcon } from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import { ServiceStatus, DatabaseStatus } from '@/types/health.types';

interface StatusCardProps {
  title: string;
  subtitle: string;
  icon: LucideIcon;
  status: ServiceStatus | DatabaseStatus;
  statusLabel: string;
  badgeText?: string;
  portOrUrl?: string;
  accentColor: 'indigo' | 'emerald' | 'cyan' | 'amber';
  infoItems?: Array<{ label: string; value: string }>;
  error?: string;
}

export function StatusCard({
  title,
  subtitle,
  icon: Icon,
  status,
  statusLabel,
  badgeText,
  portOrUrl,
  accentColor,
  infoItems = [],
  error,
}: StatusCardProps) {
  const accentClasses = {
    indigo: 'from-indigo-500/20 to-purple-500/10 text-indigo-400 border-indigo-500/30',
    emerald: 'from-emerald-500/20 to-teal-500/10 text-emerald-400 border-emerald-500/30',
    cyan: 'from-cyan-500/20 to-blue-500/10 text-cyan-400 border-cyan-500/30',
    amber: 'from-amber-500/20 to-orange-500/10 text-amber-400 border-amber-500/30',
  }[accentColor];

  return (
    <div className="glass-panel glass-panel-hover rounded-2xl p-6 relative overflow-hidden flex flex-col justify-between">
      {/* Top ambient highlight */}
      <div
        className={`absolute -top-12 -right-12 w-32 h-32 bg-gradient-to-br ${accentClasses} rounded-full blur-2xl pointer-events-none`}
      />

      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div
              className={`p-2.5 rounded-xl bg-slate-900/80 border border-slate-700/50 shadow-inner text-slate-200`}
            >
              <Icon className="w-5 h-5 text-indigo-400" />
            </div>
            <div>
              <h3 className="font-semibold text-slate-100 text-base tracking-tight">{title}</h3>
              <p className="text-xs text-slate-400">{subtitle}</p>
            </div>
          </div>
          <StatusBadge status={status} label={badgeText || statusLabel} />
        </div>

        {portOrUrl && (
          <div className="mb-4">
            <span className="text-xs font-mono px-2.5 py-1 rounded-md bg-slate-950/60 border border-slate-800 text-slate-300">
              {portOrUrl}
            </span>
          </div>
        )}

        {infoItems.length > 0 && (
          <div className="space-y-2 mb-4 pt-2 border-t border-slate-800/80">
            {infoItems.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between text-xs">
                <span className="text-slate-400">{item.label}</span>
                <span className="font-mono text-slate-200">{item.value}</span>
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="mt-3 p-3 rounded-xl bg-rose-950/40 border border-rose-800/40 text-xs text-rose-300">
            <p className="font-semibold mb-1">Connection Issue:</p>
            <p className="font-mono text-[11px] leading-relaxed break-words">{error}</p>
          </div>
        )}
      </div>

      <div className="mt-4 pt-3 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-400">
        <span>Layer Protocol</span>
        <span className="font-mono text-slate-300">
          {title.includes('Frontend')
            ? 'HTTP / Next SSR'
            : title.includes('Backend')
            ? 'REST JSON / OpenAPI'
            : 'PostgreSQL Wire 3.0'}
        </span>
      </div>
    </div>
  );
}
