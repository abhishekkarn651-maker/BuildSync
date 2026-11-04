'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  User,
  Briefcase,
  MapPin,
  Globe,
  Clock,
  Sparkles,
  Edit3,
  Calendar,
  ExternalLink,
  Share2,
  Check,
  Camera,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { GithubIcon } from '@/components/ui/Icons';
import { SkillsSection } from '@/components/profile/SkillsSection';
import { EducationSection } from '@/components/profile/EducationSection';
import { ExperienceSection } from '@/components/profile/ExperienceSection';
import { useAuth } from '@/context/AuthContext';
import { profileService } from '@/services/profile.service';
import { DeveloperProfile } from '@/types/profile.types';
import { getAvatarUrl } from '@/config/api.config';

export default function MyProfilePage() {
  const router = useRouter();
  const { user } = useAuth();
  const [profile, setProfile] = useState<DeveloperProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copiedLink, setCopiedLink] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function fetchProfile() {
      try {
        const data = await profileService.getMyProfile();
        if (isMounted) {
          setProfile(data);
        }
      } catch (err) {
        console.error('Error fetching developer profile:', err);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    fetchProfile();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleShare = () => {
    if (typeof window !== 'undefined' && profile) {
      const publicUrl = `${window.location.origin}/developers/${profile.username}`;
      navigator.clipboard.writeText(publicUrl);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    }
  };

  const formatExperienceLevel = (level?: string | null) => {
    switch (level) {
      case 'JUNIOR':
        return 'Junior Developer';
      case 'MID_LEVEL':
        return 'Mid-Level Engineer';
      case 'SENIOR':
        return 'Senior Engineer';
      case 'LEAD':
        return 'Lead / Architect';
      default:
        return level || 'Developer';
    }
  };

  const getAvailabilityBadge = (status?: string) => {
    switch (status) {
      case 'AVAILABLE':
        return <Badge variant="success" size="sm">Available for Projects</Badge>;
      case 'BUSY':
        return <Badge variant="warning" size="sm">Busy / Inquiries Only</Badge>;
      case 'MENTORING':
        return <Badge variant="primary" size="sm">Mentoring & Pairing</Badge>;
      case 'NOT_AVAILABLE':
        return <Badge variant="neutral" size="sm">Not Available</Badge>;
      default:
        return <Badge variant="success" size="sm">Available</Badge>;
    }
  };

  if (isLoading) {
    return (
      <AppLayout requireAuth>
        <div className="min-h-[400px] flex flex-col items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-border border-t-[#EF4444] animate-spin mb-3" />
          <p className="text-xs text-muted">Loading developer profile...</p>
        </div>
      </AppLayout>
    );
  }

  // Missing Profile State (HTTP 404 from backend)
  if (!profile) {
    return (
      <AppLayout requireAuth>
        <div className="max-w-2xl mx-auto py-12">
          <EmptyState
            icon={<User className="w-6 h-6 text-[#EF4444]" />}
            title="Complete Your Developer Profile"
            description="You haven't set up your developer profile yet. Share your engineering headline, background, and availability to showcase your identity."
            actionLabel="Complete Profile Now"
            onAction={() => router.push('/profile/setup')}
          />
        </div>
      </AppLayout>
    );
  }

  const displayName = profile.fullName || user?.fullName || profile.username;

  return (
    <AppLayout requireAuth>
      <div className="max-w-5xl mx-auto space-y-6 pb-12">
        {/* Profile Hero Card */}
        <div className="relative overflow-hidden rounded-2xl bg-card border border-border p-6 sm:p-8">
          {/* Subtle Cyber Ember Glow */}
          <div className="absolute -right-20 -top-20 w-64 h-64 bg-red-600/10 blur-3xl pointer-events-none rounded-full" />

          <div className="relative z-10 flex flex-col md:flex-row md:items-start justify-between gap-6">
            {/* User Info Left */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
              <div className="relative group">
                <Avatar
                  name={displayName}
                  src={getAvatarUrl(profile.avatarUrl)}
                  size="xl"
                  className="w-20 h-20 sm:w-24 sm:h-24 text-2xl font-bold ring-2 ring-[#EF4444]/40"
                />
                <button
                  type="button"
                  onClick={() => router.push('/profile/edit')}
                  className="absolute bottom-0 right-0 p-1.5 rounded-full bg-[#EF4444] text-white hover:bg-red-600 transition-transform hover:scale-110 active:scale-95 shadow-md border-2 border-card cursor-pointer"
                  title="Change profile avatar"
                  aria-label="Change profile avatar"
                >
                  <Camera className="w-3 h-3" />
                </button>
              </div>

              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2.5">
                  <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
                    {displayName}
                  </h1>
                  {getAvailabilityBadge(profile.availabilityStatus)}
                </div>

                <p className="text-sm sm:text-base font-medium text-foreground/90">
                  {profile.headline || 'Software Developer'}
                </p>

                <div className="flex flex-wrap items-center gap-4 text-xs text-muted pt-1">
                  {profile.location && (
                    <div className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5" />
                      <span>{profile.location}</span>
                    </div>
                  )}

                  <div className="flex items-center gap-1">
                    <Briefcase className="w-3.5 h-3.5" />
                    <span>{formatExperienceLevel(profile.experienceLevel)}</span>
                  </div>

                  {typeof profile.yearsOfExperience === 'number' && (
                    <div className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      <span>{profile.yearsOfExperience} yrs experience</span>
                    </div>
                  )}

                  <div className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>Joined {new Date(profile.createdAt).toLocaleDateString(undefined, { month: 'short', year: 'numeric' })}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Profile Action Buttons */}
            <div className="flex items-center gap-2.5 shrink-0 self-start">
              <Button
                variant="outline"
                size="sm"
                onClick={handleShare}
                leftIcon={copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Share2 className="w-3.5 h-3.5" />}
              >
                {copiedLink ? 'Copied Link' : 'Share Profile'}
              </Button>

              <Link href="/profile/edit">
                <Button
                  variant="primary"
                  size="sm"
                  leftIcon={<Edit3 className="w-3.5 h-3.5" />}
                >
                  Edit Profile
                </Button>
              </Link>
            </div>
          </div>
        </div>

        {/* 2-Column Content Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Main Column */}
          <div className="lg:col-span-8 space-y-6">
            {/* Bio Summary */}
            {profile.bio && (
              <Card className="p-6 space-y-2">
                <h2 className="text-sm font-bold uppercase tracking-wider text-muted">
                  Professional Summary
                </h2>
                <p className="text-sm text-foreground/90 leading-relaxed">
                  {profile.bio}
                </p>
              </Card>
            )}

            {/* Detailed Background */}
            {profile.about && (
              <Card className="p-6 space-y-2">
                <h2 className="text-sm font-bold uppercase tracking-wider text-muted">
                  About Me
                </h2>
                <p className="text-sm text-foreground/80 leading-relaxed whitespace-pre-wrap">
                  {profile.about}
                </p>
              </Card>
            )}

            {/* Skills Section */}
            <Card className="p-6">
              <SkillsSection
                skills={profile.skills || []}
                editable={true}
                onSkillsChange={(newSkills) =>
                  setProfile((prev) => (prev ? { ...prev, skills: newSkills } : null))
                }
              />
            </Card>

            {/* Experience Section */}
            <Card className="p-6">
              <ExperienceSection
                experience={profile.experience || []}
                editable={true}
                onExperienceChange={(newExp) =>
                  setProfile((prev) => (prev ? { ...prev, experience: newExp } : null))
                }
              />
            </Card>

            {/* Education Section */}
            <Card className="p-6">
              <EducationSection
                education={profile.education || []}
                editable={true}
                onEducationChange={(newEdu) =>
                  setProfile((prev) => (prev ? { ...prev, education: newEdu } : null))
                }
              />
            </Card>
          </div>

          {/* Sidebar Column */}
          <div className="lg:col-span-4 space-y-6">
            {/* Online Links Card */}
            <Card className="p-5 space-y-4">
              <h2 className="text-xs font-bold uppercase tracking-wider text-muted">
                Connect & Code
              </h2>

              <div className="space-y-2 text-xs">
                {profile.githubUrl && (
                  <a
                    href={profile.githubUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between p-2.5 rounded-lg bg-surface hover:bg-surface-hover text-foreground transition-colors group"
                  >
                    <div className="flex items-center gap-2.5">
                      <GithubIcon className="w-4 h-4 text-muted group-hover:text-foreground" />
                      <span className="font-medium">GitHub</span>
                    </div>
                    <ExternalLink className="w-3.5 h-3.5 text-muted group-hover:text-foreground" />
                  </a>
                )}

                {profile.websiteUrl && (
                  <a
                    href={profile.websiteUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between p-2.5 rounded-lg bg-surface hover:bg-surface-hover text-foreground transition-colors group"
                  >
                    <div className="flex items-center gap-2.5">
                      <Globe className="w-4 h-4 text-muted group-hover:text-foreground" />
                      <span className="font-medium">Website / Portfolio</span>
                    </div>
                    <ExternalLink className="w-3.5 h-3.5 text-muted group-hover:text-foreground" />
                  </a>
                )}

                {profile.linkedinUrl && (
                  <a
                    href={profile.linkedinUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between p-2.5 rounded-lg bg-surface hover:bg-surface-hover text-foreground transition-colors group"
                  >
                    <div className="flex items-center gap-2.5">
                      <Briefcase className="w-4 h-4 text-muted group-hover:text-foreground" />
                      <span className="font-medium">LinkedIn</span>
                    </div>
                    <ExternalLink className="w-3.5 h-3.5 text-muted group-hover:text-foreground" />
                  </a>
                )}

                {profile.twitterUrl && (
                  <a
                    href={profile.twitterUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between p-2.5 rounded-lg bg-surface hover:bg-surface-hover text-foreground transition-colors group"
                  >
                    <div className="flex items-center gap-2.5">
                      <Sparkles className="w-4 h-4 text-muted group-hover:text-foreground" />
                      <span className="font-medium">Twitter / X</span>
                    </div>
                    <ExternalLink className="w-3.5 h-3.5 text-muted group-hover:text-foreground" />
                  </a>
                )}

                {!profile.githubUrl && !profile.websiteUrl && !profile.linkedinUrl && !profile.twitterUrl && (
                  <p className="text-xs text-muted py-1">
                    No external profiles added yet.
                  </p>
                )}
              </div>
            </Card>

            {/* GitHub Showcase Card */}
            <Card className="p-5 space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-xs font-bold uppercase tracking-wider text-muted flex items-center gap-1.5">
                  <GithubIcon className="w-3.5 h-3.5 text-foreground" />
                  <span>GitHub Integration</span>
                </h2>
                <Badge variant="primary" size="sm" className="text-[10px]">
                  Phase 7
                </Badge>
              </div>
              <p className="text-xs text-muted">
                Sync repositories and toggle which public projects are showcased on your public developer profile.
              </p>
              <div className="pt-2">
                <Link href="/settings/integrations/github">
                  <Button variant="outline" size="sm" className="w-full text-xs flex items-center justify-center gap-1.5">
                    <span>Manage GitHub Showcase</span>
                    <ExternalLink className="w-3 h-3" />
                  </Button>
                </Link>
              </div>
            </Card>

            {/* Account Details Card */}
            <Card className="p-5 space-y-3">
              <h2 className="text-xs font-bold uppercase tracking-wider text-muted">
                Account Details
              </h2>
              <div className="space-y-2 text-xs">
                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted">Email</span>
                  <span className="font-medium text-foreground">{profile.email}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-border/50">
                  <span className="text-muted">Username</span>
                  <span className="font-mono text-foreground">{profile.username}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-muted">Last Updated</span>
                  <span className="text-muted">{new Date(profile.updatedAt).toLocaleDateString()}</span>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </AppLayout>
  );
}
