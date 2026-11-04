import React from 'react';
import Link from 'next/link';
import { ArrowRight, UserPlus, FileCode, MessageSquare } from 'lucide-react';
import { MOCK_NOTIFICATIONS } from '@/lib/mock-data';

export function NotificationsPanel() {
  const getIcon = (type: string) => {
    switch (type) {
      case 'collaboration':
        return <UserPlus className="w-3.5 h-3.5 text-[#EF4444]" />;
      case 'project':
        return (
          <span className="font-mono text-xs font-bold text-[#EF4444]">
            &lt;/&gt;
          </span>
        );
      case 'message':
        return <MessageSquare className="w-3.5 h-3.5 text-[#EF4444]" />;
      default:
        return <FileCode className="w-3.5 h-3.5 text-[#EF4444]" />;
    }
  };

  const displayList = MOCK_NOTIFICATIONS.slice(0, 3);

  return (
    <div className="rounded-xl bg-card border border-border p-5 space-y-4">
      {/* Header with View All -> */}
      <div className="flex items-center justify-between border-b border-border pb-3">
        <h2 className="text-sm sm:text-base font-bold text-foreground tracking-tight">
          Notifications
        </h2>
        <Link
          href="/notifications"
          className="inline-flex items-center gap-1 text-xs font-semibold text-[#EF4444] hover:text-red-400 transition-colors"
        >
          <span>View All</span>
          <ArrowRight className="w-3 h-3" />
        </Link>
      </div>

      {/* Notifications list */}
      <div className="space-y-3">
        {displayList.map((item) => (
          <Link
            key={item.id}
            href={item.link || '/notifications'}
            className="flex items-start justify-between gap-3 p-2.5 rounded-lg hover:bg-surface transition-colors group"
          >
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0 mt-0.5">
                {getIcon(item.type)}
              </div>
              <div>
                <h3 className="text-xs font-semibold text-foreground group-hover:text-[#EF4444] transition-colors leading-tight">
                  {item.title}
                </h3>
                <p className="text-[11px] text-muted mt-0.5 line-clamp-1">
                  {item.description}
                </p>
              </div>
            </div>
            <span className="text-[10px] text-muted shrink-0 font-medium whitespace-nowrap">
              {item.timeAgo}
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}

