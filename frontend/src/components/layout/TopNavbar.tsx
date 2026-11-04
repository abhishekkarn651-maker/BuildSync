'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Menu,
  Moon,
  Sun,
  Bell,
  ChevronDown,
  User,
  Edit3,
  Settings,
  LogOut,
} from 'lucide-react';
import { SearchInput } from '../ui/SearchInput';
import { Avatar } from '../ui/Avatar';
import { Dropdown } from '../ui/Dropdown';
import { useTheme } from '@/context/ThemeContext';
import { useAuth } from '@/context/AuthContext';
import { getAvatarUrl } from '@/config/api.config';

export interface TopNavbarProps {
  onOpenMobileMenu?: () => void;
  onSearch?: (query: string) => void;
}

export function TopNavbar({ onOpenMobileMenu }: TopNavbarProps) {
  const { resolvedTheme, toggleTheme, setTheme } = useTheme();
  const { user, isAuthenticated, logout } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const router = useRouter();

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/developers?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      router.push('/login');
    } catch {
      router.push('/login');
    }
  };

  const userMenuItems = [
    {
      id: 'profile',
      label: 'My Profile',
      icon: <User className="w-4 h-4" />,
      onClick: () => router.push('/profile'),
    },
    {
      id: 'edit-profile',
      label: 'Edit Profile',
      icon: <Edit3 className="w-4 h-4" />,
      onClick: () => router.push('/profile/edit'),
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: <Settings className="w-4 h-4" />,
      onClick: () => router.push('/settings'),
    },
    {
      id: 'divider-1',
      label: '',
      divider: true,
    },
    {
      id: 'logout',
      label: 'Sign Out',
      icon: <LogOut className="w-4 h-4" />,
      danger: true,
      onClick: handleLogout,
    },
  ];

  const displayName = user?.fullName || user?.username || 'Bharat Kumar';

  return (
    <header className="sticky top-0 z-20 h-16 w-full bg-card/90 backdrop-blur-md border-b border-border px-4 sm:px-6 flex items-center justify-between gap-4 select-none">
      {/* Mobile Menu Trigger & Search */}
      <div className="flex items-center gap-3 flex-1 max-w-xl">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="md:hidden p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface transition-colors"
          aria-label="Open navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <form onSubmit={handleSearchSubmit} className="w-full">
          <SearchInput
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onClear={() => setSearchQuery('')}
            placeholder="Search developers, projects, technologies..."
          />
        </form>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Quick Theme Toggle Icon */}
        <button
          type="button"
          onClick={toggleTheme}
          className="p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface transition-colors"
          title={`Switch to ${resolvedTheme === 'dark' ? 'Light' : 'Dark'} Mode`}
          aria-label="Toggle color theme"
        >
          {resolvedTheme === 'dark' ? (
            <Moon className="w-4 h-4" />
          ) : (
            <Sun className="w-4 h-4 text-amber-500" />
          )}
        </button>

        {/* Notifications Icon with unread red badge */}
        <Link
          href="/notifications"
          className="relative p-2 rounded-lg text-muted hover:text-foreground hover:bg-surface transition-colors"
          title="Notifications"
          aria-label="Notifications"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#EF4444] ring-2 ring-card animate-pulse" />
        </Link>

        {/* User Profile dropdown chip or Sign In button */}
        {isAuthenticated && user ? (
          <Dropdown
            align="right"
            trigger={
              <button
                type="button"
                className="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-surface transition-colors cursor-pointer text-left"
              >
                <Avatar
                  name={displayName}
                  src={getAvatarUrl(user?.avatarUrl)}
                  size="sm"
                />
                <span className="hidden sm:inline-block text-xs font-semibold text-foreground tracking-tight max-w-[120px] truncate">
                  {displayName}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-muted hidden sm:inline-block" />
              </button>
            }
            items={userMenuItems}
          />
        ) : (
          <div className="flex items-center gap-2">
            <Link
              href="/login"
              className="text-xs font-semibold px-3 py-1.5 rounded-lg text-foreground hover:bg-surface transition-colors"
            >
              Sign In
            </Link>
            <Link
              href="/register"
              className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-[#EF4444] text-white hover:bg-red-600 transition-colors shadow-sm shadow-red-500/20"
            >
              Sign Up
            </Link>
          </div>
        )}

        {/* Theme pill selector matching mockup: [ 🌙 Dark Mode ] */}
        <button
          type="button"
          onClick={() => setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')}
          className="hidden lg:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-surface hover:bg-surface-hover text-foreground border border-border transition-colors cursor-pointer"
        >
          {resolvedTheme === 'dark' ? (
            <>
              <Moon className="w-3.5 h-3.5 text-[#EF4444]" />
              <span>Dark Mode</span>
            </>
          ) : (
            <>
              <Sun className="w-3.5 h-3.5 text-[#EF4444]" />
              <span>Light Mode</span>
            </>
          )}
        </button>
      </div>
    </header>
  );
}
