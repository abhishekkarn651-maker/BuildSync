'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  Moon,
  Sun,
  Laptop,
  CheckCircle2,
  Save,
  ExternalLink,
  ShieldCheck,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Tabs } from '@/components/ui/Tabs';
import { GithubIcon } from '@/components/ui/Icons';
import { useTheme } from '@/context/ThemeContext';
import { CURRENT_USER } from '@/lib/mock-data';

type SettingsTab = 'appearance' | 'profile' | 'integrations' | 'notifications' | 'privacy';

export default function SettingsPage() {
  const { theme, setTheme, resolvedTheme } = useTheme();
  const [activeTab, setActiveTab] = useState<SettingsTab>('appearance');

  const [name, setName] = useState(CURRENT_USER.name);
  const [username, setUsername] = useState(CURRENT_USER.username);
  const [bio, setBio] = useState(CURRENT_USER.bio);
  const [location, setLocation] = useState(CURRENT_USER.location);
  const [githubUrl, setGithubUrl] = useState(CURRENT_USER.githubUrl);

  const [notifyCollabs, setNotifyCollabs] = useState(true);
  const [notifyMessages, setNotifyMessages] = useState(true);
  const [notifyProjects, setNotifyProjects] = useState(false);

  const [savedFeedback, setSavedFeedback] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSavedFeedback(true);
    setTimeout(() => setSavedFeedback(false), 2000);
  };

  return (
    <AppLayout requireAuth>
      <div className="space-y-6 max-w-4xl">
        {/* Header */}
        <div className="border-b border-border pb-5">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Settings & Preferences
          </h1>
          <p className="text-xs sm:text-sm text-muted mt-1">
            Configure your workspace appearance, personal profile information, and notification rules.
          </p>
        </div>

        {/* Tabs */}
        <Tabs
          tabs={[
            { id: 'appearance', label: 'Appearance & Theme' },
            { id: 'profile', label: 'Profile Details' },
            { id: 'integrations', label: 'GitHub & Integrations' },
            { id: 'notifications', label: 'Notifications' },
            { id: 'privacy', label: 'Account & Privacy' },
          ]}
          activeTab={activeTab}
          onChange={(tab) => setActiveTab(tab as SettingsTab)}
        />

        {/* Saved Feedback Toast */}
        {savedFeedback && (
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-500 font-semibold flex items-center gap-2 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4" />
            <span>Preferences saved successfully to local storage!</span>
          </div>
        )}

        {/* Tab 1: Appearance & Theme */}
        {activeTab === 'appearance' && (
          <div className="p-6 rounded-2xl bg-card border border-border space-y-6">
            <div>
              <h3 className="text-base font-bold text-foreground">
                Interface Color Palette
              </h3>
              <p className="text-xs text-muted mt-1">
                BuildSync uses the locked <strong className="text-[#EF4444]">Cyber Ember Red (#05)</strong> design system with high-contrast surfaces.
              </p>
            </div>

            {/* 3 Theme Options Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {/* Dark Mode */}
              <button
                type="button"
                onClick={() => setTheme('dark')}
                className={`p-5 rounded-xl border text-left transition-all cursor-pointer flex flex-col justify-between space-y-4 ${
                  theme === 'dark'
                    ? 'bg-red-500/10 border-[#EF4444] shadow-md shadow-red-500/10'
                    : 'bg-surface border-border hover:border-border/80'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-800 text-[#EF4444] flex items-center justify-center">
                    <Moon className="w-5 h-5" />
                  </div>
                  {theme === 'dark' && (
                    <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                  )}
                </div>
                <div>
                  <h4 className="text-xs font-bold text-foreground">Dark Charcoal</h4>
                  <p className="text-[11px] text-muted mt-0.5">
                    Background #0A0A0A • Card #1F1F1F
                  </p>
                </div>
              </button>

              {/* Light Mode */}
              <button
                type="button"
                onClick={() => setTheme('light')}
                className={`p-5 rounded-xl border text-left transition-all cursor-pointer flex flex-col justify-between space-y-4 ${
                  theme === 'light'
                    ? 'bg-red-500/10 border-[#EF4444] shadow-md shadow-red-500/10'
                    : 'bg-surface border-border hover:border-border/80'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-lg bg-white border border-zinc-200 text-[#EF4444] flex items-center justify-center">
                    <Sun className="w-5 h-5 text-amber-500" />
                  </div>
                  {theme === 'light' && (
                    <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                  )}
                </div>
                <div>
                  <h4 className="text-xs font-bold text-foreground">Clean Crisp Light</h4>
                  <p className="text-[11px] text-muted mt-0.5">
                    Background #FAFAFA • Card #FFFFFF
                  </p>
                </div>
              </button>

              {/* System Preference */}
              <button
                type="button"
                onClick={() => setTheme('system')}
                className={`p-5 rounded-xl border text-left transition-all cursor-pointer flex flex-col justify-between space-y-4 ${
                  theme === 'system'
                    ? 'bg-red-500/10 border-[#EF4444] shadow-md shadow-red-500/10'
                    : 'bg-surface border-border hover:border-border/80'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-lg bg-surface border border-border text-foreground flex items-center justify-center">
                    <Laptop className="w-5 h-5" />
                  </div>
                  {theme === 'system' && (
                    <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                  )}
                </div>
                <div>
                  <h4 className="text-xs font-bold text-foreground">Sync with OS</h4>
                  <p className="text-[11px] text-muted mt-0.5">
                    Currently: {resolvedTheme} mode
                  </p>
                </div>
              </button>
            </div>

            <div className="p-4 rounded-xl bg-surface border border-border text-xs text-muted flex items-center justify-between">
              <div>
                <span className="font-semibold text-foreground">Active Theme:</span>{' '}
                <span className="text-[#EF4444] font-medium capitalize">{theme}</span>{' '}
                (resolved to {resolvedTheme})
              </div>
              <span className="text-[11px] font-mono bg-card px-2 py-0.5 rounded border border-border">
                localStorage: buildsync-theme
              </span>
            </div>
          </div>
        )}

        {/* Tab 2: Profile Settings */}
        {activeTab === 'profile' && (
          <form onSubmit={handleSave} className="p-6 rounded-2xl bg-card border border-border space-y-5">
            <div>
              <h3 className="text-base font-bold text-foreground">
                Developer Profile Details
              </h3>
              <p className="text-xs text-muted mt-1">
                Update how your profile appears on the Discovery and Showcase pages.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Display Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <Input
                label="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <Input
              label="Location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-foreground tracking-wide">
                Biography
              </label>
              <textarea
                className="w-full h-24 rounded-lg bg-surface border border-border px-3.5 py-2 text-sm text-foreground placeholder:text-muted/60 transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
              />
            </div>

            <Input
              label="GitHub Profile URL"
              value={githubUrl}
              onChange={(e) => setGithubUrl(e.target.value)}
              leftIcon={<GithubIcon className="w-4 h-4" />}
            />

            <div className="pt-2 flex justify-end">
              <Button type="submit" variant="primary" size="sm" leftIcon={<Save className="w-3.5 h-3.5" />}>
                Save Profile Changes
              </Button>
            </div>
          </form>
        )}

        {/* Tab: GitHub & Integrations */}
        {activeTab === 'integrations' && (
          <div className="p-6 rounded-2xl bg-card border border-border space-y-6">
            <div>
              <h3 className="text-base font-bold text-foreground flex items-center gap-2">
                External Integrations
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/30">
                  Phase 7
                </span>
              </h3>
              <p className="text-xs text-muted mt-1">
                Link external developer tooling to synchronize repositories and showcase open-source projects.
              </p>
            </div>

            <div className="p-5 rounded-xl bg-surface border border-border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-card border border-border flex items-center justify-center text-foreground">
                  <GithubIcon className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-foreground flex items-center gap-2">
                    GitHub App Authorization
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                  </h4>
                  <p className="text-xs text-muted">
                    PKCE OAuth, AES-256-GCM token encryption, repository synchronization, and profile showcase.
                  </p>
                </div>
              </div>

              <Link href="/settings/integrations/github">
                <Button variant="primary" size="sm" className="text-xs flex items-center gap-1.5 whitespace-nowrap">
                  <span>Manage Integration</span>
                  <ExternalLink className="w-3 h-3" />
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* Tab 3: Notification Preferences */}
        {activeTab === 'notifications' && (
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5">
            <div>
              <h3 className="text-base font-bold text-foreground">
                Notification Preferences
              </h3>
              <p className="text-xs text-muted mt-1">
                Choose which platform events dispatch in-app and audio notifications.
              </p>
            </div>

            <div className="space-y-3 divide-y divide-border/60">
              <div className="flex items-center justify-between pt-3">
                <div>
                  <h4 className="text-xs font-bold text-foreground">Collaboration Invites</h4>
                  <p className="text-[11px] text-muted">Notify me when developers invite me to join projects.</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifyCollabs}
                  onChange={(e) => setNotifyCollabs(e.target.checked)}
                  className="rounded border-border text-[#EF4444] focus:ring-[#EF4444] accent-[#EF4444]"
                />
              </div>

              <div className="flex items-center justify-between pt-3">
                <div>
                  <h4 className="text-xs font-bold text-foreground">Direct Messages</h4>
                  <p className="text-[11px] text-muted">Notify me on incoming chat messages.</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifyMessages}
                  onChange={(e) => setNotifyMessages(e.target.checked)}
                  className="rounded border-border text-[#EF4444] focus:ring-[#EF4444] accent-[#EF4444]"
                />
              </div>

              <div className="flex items-center justify-between pt-3">
                <div>
                  <h4 className="text-xs font-bold text-foreground">Project Updates</h4>
                  <p className="text-[11px] text-muted">Receive digests for milestone advancements.</p>
                </div>
                <input
                  type="checkbox"
                  checked={notifyProjects}
                  onChange={(e) => setNotifyProjects(e.target.checked)}
                  className="rounded border-border text-[#EF4444] focus:ring-[#EF4444] accent-[#EF4444]"
                />
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <Button
                variant="primary"
                size="sm"
                leftIcon={<Save className="w-3.5 h-3.5" />}
                onClick={() => {
                  setSavedFeedback(true);
                  setTimeout(() => setSavedFeedback(false), 2000);
                }}
              >
                Save Notification Rules
              </Button>
            </div>
          </div>
        )}

        {/* Tab 4: Account & Privacy */}
        {activeTab === 'privacy' && (
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5">
            <div>
              <h3 className="text-base font-bold text-foreground">
                Account & Security
              </h3>
              <p className="text-xs text-muted mt-1">
                Manage your credentials and security controls.
              </p>
            </div>

            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-surface border border-border flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold text-foreground">GitHub Integration</h4>
                  <p className="text-[11px] text-muted">Manage OAuth credentials, token status, and repo sync</p>
                </div>
                <Link href="/settings/integrations/github">
                  <Button variant="secondary" size="sm" className="text-xs">
                    Configure
                  </Button>
                </Link>
              </div>

              <div className="p-4 rounded-xl bg-surface border border-border flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold text-foreground">Export Account Data</h4>
                  <p className="text-[11px] text-muted">Download JSON archive of all projects and profile history</p>
                </div>
                <Button variant="secondary" size="sm" className="text-xs">
                  Request Export
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
