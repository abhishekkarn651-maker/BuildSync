'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Users,
  FolderCode,
  MessageSquare,
  Bell,
  User,
  Settings,
  X,
} from 'lucide-react';
import { BrandLogo } from '../ui/BrandLogo';
import { useAuth } from '@/context/AuthContext';
import { messagingService } from '@/services/messaging.service';

export interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export function Sidebar({ isOpen = false, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();
  const [unreadMessages, setUnreadMessages] = useState<number>(0);

  useEffect(() => {
    let isMounted = true;
    if (!isAuthenticated) return;

    async function loadUnread() {
      try {
        const res = await messagingService.getUnreadCount();
        if (isMounted) {
          setUnreadMessages(res.unreadCount);
        }
      } catch {
        // Silent fail
      }
    }

    loadUnread();
    const interval = setInterval(loadUnread, 30000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [isAuthenticated, pathname]);

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Find Teammates', href: '/developers', icon: Users },
    { name: 'Projects', href: '/projects', icon: FolderCode },
    {
      name: 'Messages',
      href: '/messages',
      icon: MessageSquare,
      badge: unreadMessages > 0 ? (unreadMessages > 99 ? '99+' : String(unreadMessages)) : undefined,
    },
    { name: 'Notifications', href: '/notifications', icon: Bell },
    { name: 'Profile', href: '/profile', icon: User },
  ];

  const bottomNavigation = [
    { name: 'Settings', href: '/settings', icon: Settings },
  ];

  const sidebarContent = (
    <div className="flex flex-col h-full bg-card border-r border-border select-none">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center justify-between border-b border-border">
        <BrandLogo size="md" />
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            className="md:hidden p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-surface transition-colors"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Main Navigation Links */}
      <nav className="flex-1 px-4 py-5 space-y-1.5 overflow-y-auto" aria-label="Sidebar">
        {navigation.map((item) => {
          const isActive =
            pathname === item.href ||
            (item.href !== '/dashboard' && pathname.startsWith(item.href));

          const Icon = item.icon;

          return (
            <Link
              key={item.name}
              href={item.href}
              onClick={onClose}
              className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all group ${
                isActive
                  ? 'bg-red-500/15 text-[#EF4444] border border-red-500/30 font-semibold shadow-sm'
                  : 'text-muted hover:text-foreground hover:bg-surface'
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon
                  className={`w-4 h-4 transition-colors ${
                    isActive
                      ? 'text-[#EF4444]'
                      : 'text-muted group-hover:text-foreground'
                  }`}
                />
                <span className={isActive ? 'text-foreground font-semibold' : ''}>
                  {item.name}
                </span>
              </div>
              {item.badge && (
                <span className="flex items-center justify-center min-w-[20px] h-5 px-1.5 text-[11px] font-bold rounded-full bg-[#EF4444] text-white shadow-sm shadow-red-500/30">
                  {item.badge}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      {/* Bottom Navigation */}
      <div className="p-4 border-t border-border space-y-1.5">
        {bottomNavigation.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.name}
              href={item.href}
              onClick={onClose}
              className={`flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive
                  ? 'bg-red-500/15 text-[#EF4444] border border-red-500/30 font-semibold'
                  : 'text-muted hover:text-foreground hover:bg-surface'
              }`}
            >
              <Icon
                className={`w-4 h-4 ${
                  isActive ? 'text-[#EF4444]' : 'text-muted'
                }`}
              />
              <span>{item.name}</span>
            </Link>
          );
        })}
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Persistent Sidebar */}
      <aside className="hidden md:flex flex-col w-64 shrink-0 h-screen sticky top-0 z-30">
        {sidebarContent}
      </aside>

      {/* Mobile Drawer */}
      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          <div
            className="fixed inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
            onClick={onClose}
            aria-hidden="true"
          />
          <div className="relative flex flex-col w-72 max-w-[85vw] h-full shadow-2xl z-10 animate-in slide-in-from-left duration-200">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}

