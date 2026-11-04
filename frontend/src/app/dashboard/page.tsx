'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowRight, Sparkles, CheckCircle2 } from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { WelcomeBanner } from '@/components/dashboard/WelcomeBanner';
import { StatCards } from '@/components/dashboard/StatCards';
import { RecentProjects } from '@/components/dashboard/RecentProjects';
import { NotificationsPanel } from '@/components/dashboard/NotificationsPanel';
import { QuickActions } from '@/components/dashboard/QuickActions';
import { SystemHealthWidget } from '@/components/dashboard/SystemHealthWidget';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { MOCK_DEVELOPERS } from '@/lib/mock-data';
import { useAuth } from '@/context/AuthContext';
import { profileService } from '@/services/profile.service';
import { DeveloperProfile } from '@/types/profile.types';

export default function DashboardPage() {
  const { user } = useAuth();
  const [connectedDevs, setConnectedDevs] = useState<Record<string, boolean>>({});
  const [profile, setProfile] = useState<DeveloperProfile | null>(null);
  const [profileLoaded, setProfileLoaded] = useState(false);

  const actualUserName = user?.fullName || user?.username || 'Developer';

  useEffect(() => {
    let isMounted = true;

    async function loadUserProfile() {
      try {
        const data = await profileService.getMyProfile();
        if (isMounted) {
          setProfile(data);
        }
      } catch {
        // Benign missing profile or network error
      } finally {
        if (isMounted) {
          setProfileLoaded(true);
        }
      }
    }

    loadUserProfile();

    return () => {
      isMounted = false;
    };
  }, []);

  const suggestedTeammates = MOCK_DEVELOPERS.filter(
    (dev) => dev.id !== 'user-bharat-kumar'
  ).slice(0, 3);

  const handleConnectToggle = (devId: string) => {
    setConnectedDevs((prev) => ({
      ...prev,
      [devId]: !prev[devId],
    }));
  };

  return (
    <AppLayout requireAuth>
      <div className="space-y-6">
        {/* Welcome Section displaying actual authenticated user name */}
        <WelcomeBanner
          name={actualUserName}
          subtitle="Discover projects, find teammates, and build something amazing together."
        />

        {/* Profile Completion Status Card */}
        {profileLoaded && !profile && (
          <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-red-950/40 via-card to-card border border-red-500/30 p-5 sm:p-6 shadow-sm">
            <div className="absolute right-0 top-0 w-48 h-48 bg-red-600/10 blur-2xl pointer-events-none rounded-full" />
            <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="space-y-1 max-w-xl">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#EF4444]">
                  <Sparkles className="w-4 h-4" />
                  <span>Profile Incomplete</span>
                </div>
                <h3 className="text-base sm:text-lg font-bold text-foreground">
                  Complete Your Developer Profile
                </h3>
                <p className="text-xs text-muted leading-relaxed">
                  Showcase your skills, introduce yourself, and help other developers discover you for project collaborations.
                </p>
              </div>
              <Link href="/profile/setup" className="shrink-0">
                <Button variant="primary" size="md" rightIcon={<ArrowRight className="w-4 h-4" />}>
                  Complete Profile
                </Button>
              </Link>
            </div>
          </div>
        )}

        {profileLoaded && profile && (
          <div className="rounded-xl bg-card border border-border p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-500 shrink-0">
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-semibold text-foreground">
                  Developer Profile Active: <span className="text-[#EF4444] font-normal">{profile.headline || `@${profile.username}`}</span>
                </p>
                <p className="text-[11px] text-muted">
                  Your profile is published and visible in developer teammate matching.
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <Link href="/profile">
                <Button variant="outline" size="sm" className="h-8 text-xs">View Profile</Button>
              </Link>
              <Link href="/profile/edit">
                <Button variant="secondary" size="sm" className="h-8 text-xs">Edit Profile</Button>
              </Link>
            </div>
          </div>
        )}

        {/* 4 Statistics Cards */}
        <StatCards />

        {/* Main Content Layout Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left Column (Approx 65% on desktop: 8 of 12 columns) */}
          <div className="lg:col-span-8 space-y-6">
            {/* Recent Projects */}
            <RecentProjects />

            {/* Suggested Teammates Section */}
            <div className="rounded-xl bg-card border border-border p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-border pb-3">
                <div>
                  <h2 className="text-base font-bold text-foreground tracking-tight">
                    Recommended Teammates
                  </h2>
                  <p className="text-xs text-muted mt-0.5">
                    Developers matched with your active tech stack and project needs.
                  </p>
                </div>
                <Link
                  href="/developers"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-[#EF4444] hover:text-red-400 transition-colors"
                >
                  <span>Explore All</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>

              <div className="space-y-3">
                {suggestedTeammates.map((dev) => {
                  const isConnected = !!connectedDevs[dev.id];

                  return (
                    <div
                      key={dev.id}
                      className="p-3.5 rounded-xl bg-surface border border-border/60 hover:border-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <Avatar
                          src={dev.avatar}
                          name={dev.name}
                          size="md"
                        />
                        <div className="space-y-0.5">
                          <Link
                            href={`/developers/${dev.username}`}
                            className="text-xs font-bold text-foreground hover:text-[#EF4444] transition-colors"
                          >
                            {dev.name}
                          </Link>
                          <p className="text-[11px] text-muted line-clamp-1">
                            {dev.role} • {dev.location}
                          </p>
                          <div className="flex flex-wrap gap-1 pt-1">
                            {dev.skills.slice(0, 3).map((skill) => (
                              <span
                                key={skill}
                                className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-card text-muted border border-border"
                              >
                                {skill}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>

                      <Button
                        variant={isConnected ? 'outline' : 'primary'}
                        size="sm"
                        onClick={() => handleConnectToggle(dev.id)}
                        className="text-xs self-start sm:self-center shrink-0"
                      >
                        {isConnected ? 'Requested' : 'Connect'}
                      </Button>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Right Column (Approx 35% on desktop: 4 of 12 columns) */}
          <div className="lg:col-span-4 space-y-6">
            {/* Quick Actions Panel */}
            <QuickActions />

            {/* Notifications Panel */}
            <NotificationsPanel />

            {/* System Health Status Widget */}
            <SystemHealthWidget />
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
