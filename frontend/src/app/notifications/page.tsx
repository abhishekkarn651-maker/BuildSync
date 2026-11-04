'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  Bell,
  UserPlus,
  MessageSquare,
  CheckCheck,
  Clock,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Tabs } from '@/components/ui/Tabs';
import { EmptyState } from '@/components/ui/EmptyState';
import { MOCK_NOTIFICATIONS, NotificationItem } from '@/lib/mock-data';

type NotifFilter = 'all' | 'unread' | 'collaboration' | 'project';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationItem[]>(MOCK_NOTIFICATIONS);
  const [activeFilter, setActiveFilter] = useState<NotifFilter>('all');

  const handleMarkAllRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleMarkOneRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const filteredNotifications = notifications.filter((n) => {
    if (activeFilter === 'unread') return !n.read;
    if (activeFilter === 'collaboration') return n.type === 'collaboration';
    if (activeFilter === 'project') return n.type === 'project';
    return true;
  });

  const unreadCount = notifications.filter((n) => !n.read).length;

  const getIcon = (type: string) => {
    switch (type) {
      case 'collaboration':
        return <UserPlus className="w-4 h-4 text-[#EF4444]" />;
      case 'project':
        return (
          <span className="font-mono text-xs font-bold text-[#EF4444]">
            &lt;/&gt;
          </span>
        );
      case 'message':
        return <MessageSquare className="w-4 h-4 text-[#EF4444]" />;
      default:
        return <Bell className="w-4 h-4 text-[#EF4444]" />;
    }
  };

  return (
    <AppLayout requireAuth>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              Notifications
            </h1>
            <p className="text-xs sm:text-sm text-muted mt-1">
              Stay synchronized with project updates, collaboration requests, and developer interactions.
            </p>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-auto">
            <Button
              variant="outline"
              size="sm"
              className="text-xs"
              leftIcon={<CheckCheck className="w-3.5 h-3.5" />}
              onClick={handleMarkAllRead}
              disabled={unreadCount === 0}
            >
              Mark All as Read
            </Button>
          </div>
        </div>

        {/* Filters */}
        <Tabs
          tabs={[
            { id: 'all', label: 'All Notifications', count: notifications.length },
            { id: 'unread', label: 'Unread', count: unreadCount },
            { id: 'collaboration', label: 'Collaborations' },
            { id: 'project', label: 'Projects' },
          ]}
          activeTab={activeFilter}
          onChange={(tab) => setActiveFilter(tab as NotifFilter)}
        />

        {/* Notifications List */}
        {filteredNotifications.length === 0 ? (
          <EmptyState
            icon={<Bell className="w-6 h-6" />}
            title="No notifications in this filter"
            description="You are completely caught up! New updates will appear here in real time."
          />
        ) : (
          <div className="space-y-3">
            {filteredNotifications.map((notif) => (
              <div
                key={notif.id}
                onClick={() => handleMarkOneRead(notif.id)}
                className={`p-4 sm:p-5 rounded-xl border transition-all flex items-start justify-between gap-4 cursor-pointer ${
                  notif.read
                    ? 'bg-card border-border hover:border-red-500/20'
                    : 'bg-red-500/5 border-red-500/30 hover:border-red-500/50'
                }`}
              >
                <div className="flex items-start gap-3.5">
                  <div className="w-9 h-9 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center shrink-0 mt-0.5">
                    {getIcon(notif.type)}
                  </div>
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs sm:text-sm font-bold text-foreground">
                        {notif.title}
                      </h4>
                      {!notif.read && (
                        <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                      )}
                    </div>
                    <p className="text-xs text-muted leading-relaxed">
                      {notif.description}
                    </p>
                    {notif.link && (
                      <Link
                        href={notif.link}
                        className="text-xs text-[#EF4444] hover:underline font-semibold inline-block pt-1"
                      >
                        View Details →
                      </Link>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-[11px] text-muted flex items-center gap-1">
                    <Clock className="w-3 h-3 text-muted" />
                    {notif.timeAgo}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
