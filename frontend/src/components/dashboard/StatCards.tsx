import React from 'react';
import Link from 'next/link';
import { Users, Star, TrendingUp } from 'lucide-react';

export function StatCards() {
  const stats = [
    {
      id: 'projects',
      label: 'My Projects',
      value: '12',
      customIcon: (
        <span className="font-mono text-base font-bold text-[#EF4444]">
          &lt;/&gt;
        </span>
      ),
      iconBg: 'bg-red-500/10 border-red-500/20',
      href: '/projects',
    },
    {
      id: 'requests',
      label: 'Collaboration Requests',
      value: '48',
      icon: Users,
      iconColor: 'text-[#EF4444]',
      iconBg: 'bg-red-500/10 border-red-500/20',
      href: '/collaborations',
    },
    {
      id: 'saved',
      label: 'Saved Developers',
      value: '124',
      icon: Star,
      iconColor: 'text-amber-500',
      iconBg: 'bg-amber-500/10 border-amber-500/20',
      href: '/developers',
    },
    {
      id: 'connections',
      label: 'Total Connections',
      value: '1,248',
      icon: TrendingUp,
      iconColor: 'text-[#EF4444]',
      iconBg: 'bg-red-500/10 border-red-500/20',
      href: '/developers',
    },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {stats.map((stat) => {
        const Icon = stat.icon;

        return (
          <Link
            key={stat.id}
            href={stat.href}
            className="flex items-center gap-4 p-4 sm:p-5 rounded-xl bg-card border border-border hover:border-red-500/40 transition-all duration-200 group"
          >
            <div
              className={`w-11 h-11 rounded-xl flex items-center justify-center border shrink-0 transition-transform group-hover:scale-105 ${stat.iconBg}`}
            >
              {stat.customIcon ? (
                stat.customIcon
              ) : Icon ? (
                <Icon className={`w-5 h-5 ${stat.iconColor}`} />
              ) : null}
            </div>
            <div>
              <div className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
                {stat.value}
              </div>
              <div className="text-xs text-muted font-medium mt-0.5">
                {stat.label}
              </div>
            </div>
          </Link>
        );
      })}
    </div>
  );
}

