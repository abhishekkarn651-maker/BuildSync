'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Briefcase,
  MapPin,
  Globe,
  Clock,
  Sparkles,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { GithubIcon } from '@/components/ui/Icons';
import { useAuth } from '@/context/AuthContext';
import { profileService } from '@/services/profile.service';
import { ApiError } from '@/services/api-client';
import {
  AvailabilityStatus,
  CreateProfilePayload,
  ExperienceLevel,
} from '@/types/profile.types';

export default function SetupProfilePage() {
  const router = useRouter();
  const { user } = useAuth();

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [hasExistingProfile, setHasExistingProfile] = useState(false);

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

  // Check if profile already exists
  useEffect(() => {
    let isMounted = true;

    async function checkExisting() {
      try {
        const existing = await profileService.getMyProfile();
        if (isMounted && existing) {
          setHasExistingProfile(true);
        }
      } catch {
        // Benign error
      } finally {
        if (isMounted) {
          setIsLoadingProfile(false);
        }
      }
    }

    checkExisting();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    // Field-level client validation
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

    const parsedYears = yearsOfExperience.trim() ? parseInt(yearsOfExperience.trim(), 10) : undefined;
    if (parsedYears !== undefined && (isNaN(parsedYears) || parsedYears < 0 || parsedYears > 100)) {
      setErrorMessage('Years of experience must be between 0 and 100');
      return;
    }

    setIsSubmitting(true);

    const payload: CreateProfilePayload = {
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
      await profileService.createProfile(payload);
      setSuccessMessage('Developer profile created successfully! Redirecting...');
      setTimeout(() => {
        router.push('/profile');
      }, 1000);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setErrorMessage('A developer profile already exists for your account.');
          setTimeout(() => {
            router.push('/profile');
          }, 1500);
        } else {
          setErrorMessage(err.message || 'Failed to create developer profile. Please check your inputs.');
        }
      } else if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('An unexpected error occurred while saving profile.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingProfile) {
    return (
      <AppLayout requireAuth>
        <div className="min-h-[400px] flex flex-col items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-border border-t-[#EF4444] animate-spin mb-3" />
          <p className="text-xs text-muted">Checking developer profile status...</p>
        </div>
      </AppLayout>
    );
  }

  if (hasExistingProfile) {
    return (
      <AppLayout requireAuth>
        <div className="max-w-2xl mx-auto py-12 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-red-500/10 border border-red-500/30 text-[#EF4444] flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold text-foreground">You Already Have a Developer Profile</h1>
          <p className="text-xs text-muted max-w-md mx-auto">
            Your developer profile has already been established. You can view your public showcase or update your details.
          </p>
          <div className="flex items-center justify-center gap-3 pt-2">
            <Link href="/profile">
              <Button variant="outline" size="sm">View My Profile</Button>
            </Link>
            <Link href="/profile/edit">
              <Button variant="primary" size="sm">Edit Profile</Button>
            </Link>
          </div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout requireAuth>
      <div className="max-w-3xl mx-auto space-y-8 pb-12">
        {/* Header */}
        <div className="border-b border-border pb-5 space-y-1">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-[#EF4444]">
            <Sparkles className="w-4 h-4" />
            <span>Developer Onboarding</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
            Complete Your Developer Profile
          </h1>
          <p className="text-xs sm:text-sm text-muted">
            Showcase your technical background, availability, and projects to match with collaborators.
          </p>
        </div>

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
                <p className="text-[11px] text-muted mt-1">
                  A concise, impactful statement summarizing your role and technical specialization.
                </p>
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

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <Link href="/dashboard">
              <Button type="button" variant="outline" size="md">
                Skip for Now
              </Button>
            </Link>
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSubmitting}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Publish Developer Profile
            </Button>
          </div>
        </form>
      </div>
    </AppLayout>
  );
}
