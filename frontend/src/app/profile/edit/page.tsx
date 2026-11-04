'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Briefcase,
  MapPin,
  Globe,
  Clock,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Save,
  Code2,
  GraduationCap,
  User,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { Tabs } from '@/components/ui/Tabs';
import { GithubIcon } from '@/components/ui/Icons';
import { SkillsSection } from '@/components/profile/SkillsSection';
import { EducationSection } from '@/components/profile/EducationSection';
import { ExperienceSection } from '@/components/profile/ExperienceSection';
import { AvatarUpload } from '@/components/profile/AvatarUpload';
import { useAuth } from '@/context/AuthContext';
import { profileService } from '@/services/profile.service';
import { ApiError } from '@/services/api-client';
import {
  AvailabilityStatus,
  DeveloperProfile,
  ExperienceLevel,
  UpdateProfilePayload,
} from '@/types/profile.types';

export default function EditProfilePage() {
  const router = useRouter();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [profile, setProfile] = useState<DeveloperProfile | null>(null);
  const [activeTab, setActiveTab] = useState('general');

  // Form states
  const [headline, setHeadline] = useState('');
  const [bio, setBio] = useState('');
  const [about, setAbout] = useState('');
  const [location, setLocation] = useState('');
  const [experienceLevel, setExperienceLevel] = useState<ExperienceLevel>('SENIOR');
  const [yearsOfExperience, setYearsOfExperience] = useState<string>('3');
  const [availabilityStatus, setAvailabilityStatus] = useState<AvailabilityStatus>('AVAILABLE');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [githubUrl, setGithubUrl] = useState('');
  const [linkedinUrl, setLinkedinUrl] = useState('');
  const [twitterUrl, setTwitterUrl] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Fetch current profile to populate
  useEffect(() => {
    let isMounted = true;

    async function loadProfile() {
      try {
        const data = await profileService.getMyProfile();
        if (!isMounted) return;

        if (!data) {
          // If no profile exists yet, redirect to setup
          router.replace('/profile/setup');
          return;
        }

        setProfile(data);
        setHeadline(data.headline || '');
        setBio(data.bio || '');
        setAbout(data.about || '');
        setLocation(data.location || '');
        setExperienceLevel(data.experienceLevel || 'SENIOR');
        setYearsOfExperience(
          data.yearsOfExperience !== null && data.yearsOfExperience !== undefined
            ? String(data.yearsOfExperience)
            : ''
        );
        setAvailabilityStatus(data.availabilityStatus || 'AVAILABLE');
        setWebsiteUrl(data.websiteUrl || '');
        setGithubUrl(data.githubUrl || '');
        setLinkedinUrl(data.linkedinUrl || '');
        setTwitterUrl(data.twitterUrl || '');
      } catch {
        if (isMounted) {
          setErrorMessage('Failed to load profile. Please try refreshing.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      isMounted = false;
    };
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    // URL validation
    const urlPattern = /^(https?:\/\/.+)?$/i;
    if (websiteUrl.trim() && !urlPattern.test(websiteUrl.trim())) {
      setErrorMessage('Website URL must start with http:// or https://');
      return;
    }
    if (githubUrl.trim() && !urlPattern.test(githubUrl.trim())) {
      setErrorMessage('GitHub URL must start with http:// or https://');
      return;
    }
    if (linkedinUrl.trim() && !urlPattern.test(linkedinUrl.trim())) {
      setErrorMessage('LinkedIn URL must start with http:// or https://');
      return;
    }
    if (twitterUrl.trim() && !urlPattern.test(twitterUrl.trim())) {
      setErrorMessage('Twitter/X URL must start with http:// or https://');
      return;
    }

    const parsedYears = yearsOfExperience.trim()
      ? parseInt(yearsOfExperience.trim(), 10)
      : undefined;
    if (
      parsedYears !== undefined &&
      (isNaN(parsedYears) || parsedYears < 0 || parsedYears > 100)
    ) {
      setErrorMessage('Years of experience must be between 0 and 100');
      return;
    }

    setIsSubmitting(true);

    const payload: UpdateProfilePayload = {
      headline: headline.trim() || undefined,
      bio: bio.trim() || undefined,
      about: about.trim() || undefined,
      location: location.trim() || undefined,
      experienceLevel,
      yearsOfExperience: parsedYears,
      availabilityStatus,
      websiteUrl: websiteUrl.trim() || undefined,
      githubUrl: githubUrl.trim() || undefined,
      linkedinUrl: linkedinUrl.trim() || undefined,
      twitterUrl: twitterUrl.trim() || undefined,
    };

    try {
      const updated = await profileService.updateMyProfile(payload);
      setProfile((prev) => (prev ? { ...prev, ...updated } : updated));
      setSuccessMessage('Developer profile updated successfully!');
      setTimeout(() => {
        router.push('/profile');
      }, 1000);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message || 'Failed to update developer profile.');
      } else if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('An unexpected error occurred while updating profile.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <AppLayout requireAuth>
        <div className="min-h-[400px] flex flex-col items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-border border-t-[#EF4444] animate-spin mb-3" />
          <p className="text-xs text-muted">Loading profile for editing...</p>
        </div>
      </AppLayout>
    );
  }

  const tabsConfig = [
    { id: 'general', label: 'Basic Information', icon: <User className="w-3.5 h-3.5" /> },
    {
      id: 'skills',
      label: 'Technical Skills',
      count: profile?.skills?.length || 0,
      icon: <Code2 className="w-3.5 h-3.5" />,
    },
    {
      id: 'experience',
      label: 'Work Experience',
      count: profile?.experience?.length || 0,
      icon: <Briefcase className="w-3.5 h-3.5" />,
    },
    {
      id: 'education',
      label: 'Education History',
      count: profile?.education?.length || 0,
      icon: <GraduationCap className="w-3.5 h-3.5" />,
    },
  ];

  return (
    <AppLayout requireAuth>
      <div className="max-w-4xl mx-auto space-y-8 pb-12">
        {/* Navigation & Header */}
        <div className="border-b border-border pb-5 space-y-2">
          <Link
            href="/profile"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to My Profile</span>
          </Link>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
                Edit Developer Profile
              </h1>
              <p className="text-xs sm:text-sm text-muted">
                Manage your professional identity, technical skills, employment, and education history.
              </p>
            </div>
            <span className="text-xs font-mono text-[#EF4444] bg-red-500/10 px-2.5 py-1 rounded-md self-start sm:self-auto">
              @{profile?.username}
            </span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <Tabs tabs={tabsConfig} activeTab={activeTab} onChange={setActiveTab} />

        {/* Tab 1: Basic Information */}
        {activeTab === 'general' && (
          <div className="space-y-6">
            {/* Feedback alerts */}
            {errorMessage && (
              <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-500 font-medium flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {successMessage && (
              <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-500 font-medium flex items-start gap-2.5">
                <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{successMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-8">
              {/* Section 0: Profile Avatar */}
              <Card className="p-6">
                <AvatarUpload
                  currentAvatarUrl={profile?.avatarUrl}
                  fullName={profile?.fullName || user?.fullName}
                  username={profile?.username || user?.username || ''}
                  onAvatarUpdated={(updated) => setProfile(updated)}
                />
              </Card>

              {/* Section 1: Professional Information */}
              <Card className="p-6 space-y-5">
                <div className="flex items-center gap-2.5 border-b border-border pb-3">
                  <Briefcase className="w-4 h-4 text-[#EF4444]" />
                  <h2 className="text-sm font-bold text-foreground tracking-tight">
                    Professional Identity
                  </h2>
                </div>

                <div className="space-y-4">
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="text-xs font-medium text-foreground tracking-wide">
                        Professional Headline
                      </label>
                      <span className="text-[11px] text-muted">{headline.length}/150</span>
                    </div>
                    <Input
                      value={headline}
                      onChange={(e) => setHeadline(e.target.value.slice(0, 150))}
                      placeholder="e.g. Senior Full-Stack Architect | Next.js & Spring Boot"
                      required
                    />
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="text-xs font-medium text-foreground tracking-wide">
                        Short Biography
                      </label>
                      <span className="text-[11px] text-muted">{bio.length}/500</span>
                    </div>
                    <textarea
                      value={bio}
                      onChange={(e) => setBio(e.target.value.slice(0, 500))}
                      rows={3}
                      className="w-full rounded-lg bg-surface border border-border px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted/60 transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40 resize-y"
                      placeholder="Tell other developers about your engineering passions, core competencies, and recent projects..."
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-foreground tracking-wide mb-1.5">
                      About Me & Engineering Journey
                    </label>
                    <textarea
                      value={about}
                      onChange={(e) => setAbout(e.target.value)}
                      rows={5}
                      className="w-full rounded-lg bg-surface border border-border px-3.5 py-2.5 text-sm text-foreground placeholder:text-muted/60 transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40 resize-y font-sans leading-relaxed"
                      placeholder="Share a more detailed background, software architecture philosophies, and the types of projects you are seeking..."
                    />
                  </div>
                </div>
              </Card>

              {/* Section 2: Experience & Collaboration */}
              <Card className="p-6 space-y-5">
                <div className="flex items-center gap-2.5 border-b border-border pb-3">
                  <Clock className="w-4 h-4 text-[#EF4444]" />
                  <h2 className="text-sm font-bold text-foreground tracking-tight">
                    Experience & Availability
                  </h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-foreground tracking-wide mb-1.5">
                      Experience Level
                    </label>
                    <select
                      value={experienceLevel}
                      onChange={(e) => setExperienceLevel(e.target.value as ExperienceLevel)}
                      className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-sm text-foreground transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40 cursor-pointer"
                    >
                      <option value="JUNIOR">Junior Developer (0-2 years)</option>
                      <option value="MID_LEVEL">Mid-Level Developer (2-5 years)</option>
                      <option value="SENIOR">Senior Engineer (5-8 years)</option>
                      <option value="LEAD">Lead / Architect (8+ years)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-foreground tracking-wide mb-1.5">
                      Years of Experience
                    </label>
                    <Input
                      type="number"
                      min="0"
                      max="100"
                      value={yearsOfExperience}
                      onChange={(e) => setYearsOfExperience(e.target.value)}
                      placeholder="e.g. 5"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-foreground tracking-wide mb-1.5">
                      Availability Status
                    </label>
                    <select
                      value={availabilityStatus}
                      onChange={(e) => setAvailabilityStatus(e.target.value as AvailabilityStatus)}
                      className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-sm text-foreground transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40 cursor-pointer"
                    >
                      <option value="AVAILABLE">Available for Projects</option>
                      <option value="BUSY">Busy (Open to Inquiries)</option>
                      <option value="MENTORING">Mentoring & Pair Programming</option>
                      <option value="NOT_AVAILABLE">Not Currently Available</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-foreground tracking-wide mb-1.5">
                      Location (City, Country)
                    </label>
                    <Input
                      value={location}
                      onChange={(e) => setLocation(e.target.value.slice(0, 100))}
                      placeholder="e.g. Bengaluru, India or Remote"
                      leftIcon={<MapPin className="w-4 h-4" />}
                    />
                  </div>
                </div>
              </Card>

              {/* Section 3: Links & Profiles */}
              <Card className="p-6 space-y-5">
                <div className="flex items-center gap-2.5 border-b border-border pb-3">
                  <Globe className="w-4 h-4 text-[#EF4444]" />
                  <h2 className="text-sm font-bold text-foreground tracking-tight">
                    Online Presence & Repositories
                  </h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Portfolio / Personal Website"
                    type="url"
                    value={websiteUrl}
                    onChange={(e) => setWebsiteUrl(e.target.value)}
                    placeholder="https://yourportfolio.dev"
                    leftIcon={<Globe className="w-4 h-4" />}
                  />

                  <Input
                    label="GitHub Profile URL"
                    type="url"
                    value={githubUrl}
                    onChange={(e) => setGithubUrl(e.target.value)}
                    placeholder={`https://github.com/${user?.username || 'username'}`}
                    leftIcon={<GithubIcon className="w-4 h-4" />}
                  />

                  <Input
                    label="LinkedIn Profile URL"
                    type="url"
                    value={linkedinUrl}
                    onChange={(e) => setLinkedinUrl(e.target.value)}
                    placeholder="https://linkedin.com/in/username"
                  />

                  <Input
                    label="Twitter / X Profile URL"
                    type="url"
                    value={twitterUrl}
                    onChange={(e) => setTwitterUrl(e.target.value)}
                    placeholder="https://x.com/username"
                  />
                </div>
              </Card>

              {/* Non-editable metadata notice */}
              <div className="p-3.5 rounded-xl bg-surface border border-border/80 text-xs text-muted flex items-center justify-between">
                <span>Account identity: <strong className="text-foreground">{user?.fullName}</strong> ({profile?.email})</span>
                <span className="text-[11px] font-mono">User ID: {user?.id?.slice(0, 8)}...</span>
              </div>

              {/* Action buttons */}
              <div className="flex items-center justify-end gap-3 pt-2">
                <Link href="/profile">
                  <Button type="button" variant="outline" size="md">
                    Cancel
                  </Button>
                </Link>
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  isLoading={isSubmitting}
                  leftIcon={<Save className="w-4 h-4" />}
                >
                  Save Basic Info
                </Button>
              </div>
            </form>
          </div>
        )}

        {/* Tab 2: Technical Skills */}
        {activeTab === 'skills' && (
          <Card className="p-6">
            <SkillsSection
              skills={profile?.skills || []}
              editable={true}
              onSkillsChange={(newSkills) =>
                setProfile((prev) => (prev ? { ...prev, skills: newSkills } : null))
              }
            />
          </Card>
        )}

        {/* Tab 3: Work Experience */}
        {activeTab === 'experience' && (
          <Card className="p-6">
            <ExperienceSection
              experience={profile?.experience || []}
              editable={true}
              onExperienceChange={(newExp) =>
                setProfile((prev) => (prev ? { ...prev, experience: newExp } : null))
              }
            />
          </Card>
        )}

        {/* Tab 4: Education History */}
        {activeTab === 'education' && (
          <Card className="p-6">
            <EducationSection
              education={profile?.education || []}
              editable={true}
              onEducationChange={(newEdu) =>
                setProfile((prev) => (prev ? { ...prev, education: newEdu } : null))
              }
            />
          </Card>
        )}
      </div>
    </AppLayout>
  );
}
