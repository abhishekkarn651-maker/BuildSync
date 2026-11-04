'use client';

import React, { useState, useEffect, useMemo, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import {
  Search,
  Users,
  Check,
  UserPlus,
  MapPin,
  Briefcase,
  X,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  AlertCircle,
  SlidersHorizontal,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { EmptyState } from '@/components/ui/EmptyState';
import { profileService } from '@/services/profile.service';
import { getAvatarUrl } from '@/config/api.config';
import {
  DeveloperSummary,
  DeveloperSortOption,
  ExperienceLevel,
  AvailabilityStatus,
} from '@/types/profile.types';

const POPULAR_SKILLS = [
  'All',
  'Java',
  'Spring Boot',
  'React',
  'Next.js',
  'TypeScript',
  'PostgreSQL',
  'Python',
  'Docker',
  'Kubernetes',
  'Tailwind CSS',
  'GraphQL',
  'AWS',
];

const EXPERIENCE_OPTIONS: { label: string; value: ExperienceLevel | '' }[] = [
  { label: 'All Experience Levels', value: '' },
  { label: 'Junior (0-2 yrs)', value: 'JUNIOR' },
  { label: 'Mid-Level (3-5 yrs)', value: 'MID_LEVEL' },
  { label: 'Senior (5-8 yrs)', value: 'SENIOR' },
  { label: 'Lead (8+ yrs)', value: 'LEAD' },
];

const AVAILABILITY_OPTIONS: { label: string; value: AvailabilityStatus | '' }[] = [
  { label: 'All Availabilities', value: '' },
  { label: 'Available Now', value: 'AVAILABLE' },
  { label: 'Open for Mentoring', value: 'MENTORING' },
  { label: 'Currently Busy', value: 'BUSY' },
  { label: 'Not Available', value: 'NOT_AVAILABLE' },
];

const SORT_OPTIONS: { label: string; value: DeveloperSortOption }[] = [
  { label: 'Recently Updated', value: 'recently_updated' },
  { label: 'Newest Profiles', value: 'newest' },
  { label: 'Name (A to Z)', value: 'name_asc' },
  { label: 'Name (Z to A)', value: 'name_desc' },
];

function DevelopersContent() {
  const searchParams = useSearchParams();

  // Read initial query parameter (?q= or ?keyword=)
  const initialKeyword = searchParams.get('q') || searchParams.get('keyword') || '';
  const initialSkill = searchParams.get('skill') || 'All';

  // Search and filter state
  const [searchInput, setSearchInput] = useState(initialKeyword);
  const [debouncedKeyword, setDebouncedKeyword] = useState(initialKeyword);
  const [selectedSkill, setSelectedSkill] = useState(initialSkill);
  const [selectedExperience, setSelectedExperience] = useState<ExperienceLevel | ''>('');
  const [selectedAvailability, setSelectedAvailability] = useState<AvailabilityStatus | ''>('');
  const [locationInput, setLocationInput] = useState('');
  const [selectedSort, setSelectedSort] = useState<DeveloperSortOption>('recently_updated');
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const pageSize = 12;

  // Data fetching state
  const [developers, setDevelopers] = useState<DeveloperSummary[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Connection interactions
  const [connectedDevs, setConnectedDevs] = useState<Record<string, boolean>>({});

  // Debounce search input (350ms)
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(searchInput);
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Sync debounced search to URL search param cleanly without page reload
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedKeyword.trim()) {
      params.set('q', debouncedKeyword.trim());
    }
    if (selectedSkill && selectedSkill !== 'All') {
      params.set('skill', selectedSkill);
    }
    const newQs = params.toString();
    const targetUrl = newQs ? `/developers?${newQs}` : '/developers';
    window.history.replaceState(null, '', targetUrl);
  }, [debouncedKeyword, selectedSkill]);

  // Fetch developers from backend
  useEffect(() => {
    let isMounted = true;

    async function executeSearch() {
      setIsLoading(true);
      setError(null);

      try {
        const response = await profileService.searchDevelopers({
          keyword: debouncedKeyword.trim() || undefined,
          skill: selectedSkill !== 'All' ? selectedSkill : undefined,
          experienceLevel: selectedExperience || undefined,
          availability: selectedAvailability || undefined,
          location: locationInput.trim() || undefined,
          page,
          size: pageSize,
          sort: selectedSort,
        });

        if (isMounted) {
          setDevelopers(response.content || []);
          setTotalElements(response.totalElements || 0);
          setTotalPages(response.totalPages || 0);
        }
      } catch (err: unknown) {
        if (isMounted) {
          console.error('Failed to load developers:', err);
          setError('Unable to load developer profiles. Please verify the backend connection and try again.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    executeSearch();

    return () => {
      isMounted = false;
    };
  }, [
    debouncedKeyword,
    selectedSkill,
    selectedExperience,
    selectedAvailability,
    locationInput,
    page,
    pageSize,
    selectedSort,
    reloadKey,
  ]);

  // Connect toggle interaction
  const handleConnectToggle = (devId: string) => {
    setConnectedDevs((prev) => ({
      ...prev,
      [devId]: !prev[devId],
    }));
  };

  // Reset all filters
  const handleResetFilters = () => {
    setSearchInput('');
    setDebouncedKeyword('');
    setSelectedSkill('All');
    setSelectedExperience('');
    setSelectedAvailability('');
    setLocationInput('');
    setSelectedSort('recently_updated');
    setPage(0);
  };

  const isFilterActive = useMemo(() => {
    return (
      debouncedKeyword.trim().length > 0 ||
      selectedSkill !== 'All' ||
      selectedExperience !== '' ||
      selectedAvailability !== '' ||
      locationInput.trim().length > 0 ||
      selectedSort !== 'recently_updated'
    );
  }, [
    debouncedKeyword,
    selectedSkill,
    selectedExperience,
    selectedAvailability,
    locationInput,
    selectedSort,
  ]);

  const formatExperience = (level?: ExperienceLevel | null, years?: number | null) => {
    let text = level ? level.replace('_', ' ') : 'Generalist';
    // Title Case
    text = text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
    if (years !== undefined && years !== null && years > 0) {
      return `${text} • ${years}y exp`;
    }
    return text;
  };

  const getAvailabilityBadgeStyle = (status: AvailabilityStatus) => {
    switch (status) {
      case 'AVAILABLE':
        return 'bg-emerald-500/10 text-emerald-500 border-emerald-500/30';
      case 'MENTORING':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      case 'BUSY':
        return 'bg-amber-500/10 text-amber-500 border-amber-500/30';
      case 'NOT_AVAILABLE':
      default:
        return 'bg-surface text-muted border-border';
    }
  };

  const formatAvailabilityLabel = (status: AvailabilityStatus) => {
    switch (status) {
      case 'AVAILABLE':
        return 'Available';
      case 'MENTORING':
        return 'Mentoring';
      case 'BUSY':
        return 'Busy';
      case 'NOT_AVAILABLE':
        return 'Unavailable';
      default:
        return status;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Find Teammates
          </h1>
          <p className="text-xs sm:text-sm text-muted mt-1">
            Discover and connect with developers based on verified technical competencies, availability, and project experience.
          </p>
        </div>
        <div className="flex items-center gap-3 self-start sm:self-auto">
          {isFilterActive && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleResetFilters}
              leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
              className="text-xs text-muted hover:text-foreground"
            >
              Reset Filters
            </Button>
          )}
          <div className="text-xs text-muted font-medium bg-surface px-3 py-1.5 rounded-lg border border-border">
            {isLoading ? (
              <span>Searching...</span>
            ) : (
              <span>
                Found <strong className="text-foreground">{totalElements}</strong> {totalElements === 1 ? 'developer' : 'developers'}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Filters and Search Bar Container */}
      <div className="p-4 sm:p-5 rounded-xl bg-card border border-border space-y-4 shadow-sm">
        {/* Primary Row: Keyword Search & Core Filters */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
          {/* Search Input */}
          <div className="md:col-span-5 relative">
            <div className="relative flex items-center w-full">
              <div className="absolute left-3.5 flex items-center pointer-events-none text-muted">
                <Search className="w-4 h-4" />
              </div>
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Search by name, username, headline, or bio..."
                className="w-full h-10 pl-10 pr-10 rounded-lg bg-surface border border-border text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/30 transition-colors"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput('')}
                  className="absolute right-3 p-1 rounded-md text-muted hover:text-foreground transition-colors"
                  aria-label="Clear search"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>

          {/* Experience Filter */}
          <div className="md:col-span-2">
            <select
              value={selectedExperience}
              onChange={(e) => {
                setSelectedExperience(e.target.value as ExperienceLevel | '');
                setPage(0);
              }}
              className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
              aria-label="Filter by experience level"
            >
              {EXPERIENCE_OPTIONS.map((opt) => (
                <option key={opt.label} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Availability Filter */}
          <div className="md:col-span-2">
            <select
              value={selectedAvailability}
              onChange={(e) => {
                setSelectedAvailability(e.target.value as AvailabilityStatus | '');
                setPage(0);
              }}
              className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
              aria-label="Filter by availability"
            >
              {AVAILABILITY_OPTIONS.map((opt) => (
                <option key={opt.label} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Sort By Filter */}
          <div className="md:col-span-3">
            <select
              value={selectedSort}
              onChange={(e) => {
                setSelectedSort(e.target.value as DeveloperSortOption);
                setPage(0);
              }}
              className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
              aria-label="Sort developers"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  Sort: {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Popular Skills Fast Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-1 text-xs scrollbar-none">
          <span className="text-muted font-medium mr-1 text-[11px] shrink-0 flex items-center gap-1">
            <SlidersHorizontal className="w-3 h-3 text-[#EF4444]" />
            Skills:
          </span>
          {POPULAR_SKILLS.map((skill) => {
            const isSelected = selectedSkill.toLowerCase() === skill.toLowerCase();
            return (
              <button
                key={skill}
                type="button"
                onClick={() => {
                  setSelectedSkill(skill);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition-all whitespace-nowrap cursor-pointer ${
                  isSelected
                    ? 'bg-red-500/15 text-[#EF4444] border border-red-500/40 font-semibold shadow-xs'
                    : 'bg-surface text-muted hover:text-foreground hover:bg-surface-hover border border-border'
                }`}
              >
                {skill}
              </button>
            );
          })}
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-start justify-between gap-3 text-xs text-red-500">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={() => setReloadKey((k) => k + 1)} className="text-xs h-7">
            Retry
          </Button>
        </div>
      )}

      {/* Content Area */}
      {isLoading ? (
        /* Loading Skeleton Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div
              key={idx}
              className="p-5 rounded-xl bg-card border border-border flex flex-col justify-between space-y-4 animate-pulse"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-surface border border-border" />
                    <div className="space-y-1.5">
                      <div className="w-28 h-4 rounded bg-surface" />
                      <div className="w-20 h-3 rounded bg-surface" />
                    </div>
                  </div>
                  <div className="w-16 h-5 rounded-full bg-surface" />
                </div>
                <div className="w-3/4 h-3.5 rounded bg-surface" />
                <div className="w-1/2 h-3 rounded bg-surface" />
                <div className="space-y-1 pt-2">
                  <div className="w-full h-3 rounded bg-surface" />
                  <div className="w-5/6 h-3 rounded bg-surface" />
                </div>
                <div className="flex gap-1.5 pt-1">
                  <div className="w-14 h-5 rounded bg-surface" />
                  <div className="w-16 h-5 rounded bg-surface" />
                  <div className="w-12 h-5 rounded bg-surface" />
                </div>
              </div>
              <div className="pt-3 border-t border-border flex justify-between items-center">
                <div className="w-16 h-7 rounded bg-surface" />
                <div className="w-20 h-7 rounded bg-surface" />
              </div>
            </div>
          ))}
        </div>
      ) : developers.length === 0 ? (
        /* Empty Results State */
        <EmptyState
          icon={<Users className="w-6 h-6 text-[#EF4444]" />}
          title="No developers matched your filters"
          description="We couldn't find any developers matching your active search and filter criteria. Try broadening your keywords or resetting filters."
          actionLabel="Reset All Filters"
          onAction={handleResetFilters}
        />
      ) : (
        /* Developers Results Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {developers.map((dev) => {
            const isConnected = !!connectedDevs[dev.id];
            const resolvedAvatar = getAvatarUrl(dev.avatarUrl);

            return (
              <div
                key={dev.id}
                className="p-5 rounded-xl bg-card border border-border flex flex-col justify-between space-y-4 hover:border-red-500/40 hover:shadow-lg hover:shadow-red-500/5 transition-all duration-200 group"
              >
                <div className="space-y-3">
                  {/* Header: Avatar, Name, Availability */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <Avatar
                        src={resolvedAvatar}
                        name={dev.fullName || dev.username}
                        size="md"
                        isOnline={dev.availabilityStatus === 'AVAILABLE'}
                      />
                      <div className="min-w-0">
                        <Link
                          href={`/developers/${encodeURIComponent(dev.username)}`}
                          className="text-sm font-bold text-foreground hover:text-[#EF4444] transition-colors truncate block group-hover:text-[#EF4444]"
                        >
                          {dev.fullName || dev.username}
                        </Link>
                        <p className="text-xs text-muted truncate">
                          @{dev.username}
                        </p>
                      </div>
                    </div>

                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border shrink-0 ${getAvailabilityBadgeStyle(
                        dev.availabilityStatus
                      )}`}
                    >
                      {formatAvailabilityLabel(dev.availabilityStatus)}
                    </span>
                  </div>

                  {/* Headline & Experience */}
                  <div className="space-y-1 text-xs">
                    <div className="flex items-center gap-1.5 text-foreground font-medium truncate">
                      <Briefcase className="w-3.5 h-3.5 text-[#EF4444] shrink-0" />
                      <span className="truncate">{dev.headline || 'Software Engineer'}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-muted truncate">
                      <MapPin className="w-3.5 h-3.5 shrink-0 text-muted" />
                      <span className="truncate">{dev.location || 'Remote'}</span>
                      <span className="mx-1">•</span>
                      <span>{formatExperience(dev.experienceLevel, dev.yearsOfExperience)}</span>
                    </div>
                  </div>

                  {/* Bio snippet */}
                  <p className="text-xs text-muted line-clamp-2 leading-relaxed">
                    {dev.bio || 'Developer on BuildSync collaborating on open source and innovative tech projects.'}
                  </p>

                  {/* Skills Pills */}
                  <div className="flex flex-wrap gap-1 pt-1 min-h-[26px]">
                    {dev.skills && dev.skills.length > 0 ? (
                      dev.skills.slice(0, 4).map((skillItem) => (
                        <span
                          key={skillItem.id || skillItem.skillId}
                          className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-surface text-muted-foreground border border-border group-hover:border-border/80 transition-colors"
                          title={skillItem.proficiencyLevel ? `Proficiency: ${skillItem.proficiencyLevel}` : undefined}
                        >
                          {skillItem.name}
                        </span>
                      ))
                    ) : (
                      <span className="text-[10px] text-muted italic">No public skills listed</span>
                    )}
                    {dev.skills && dev.skills.length > 4 && (
                      <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-md bg-surface text-muted border border-border">
                        +{dev.skills.length - 4} more
                      </span>
                    )}
                  </div>
                </div>

                {/* Footer Actions */}
                <div className="pt-3 border-t border-border flex items-center justify-between gap-2">
                  <Link href={`/developers/${encodeURIComponent(dev.username)}`}>
                    <Button variant="ghost" size="sm" className="text-xs font-medium">
                      View Profile
                    </Button>
                  </Link>

                  <Button
                    variant={isConnected ? 'secondary' : 'primary'}
                    size="sm"
                    className="text-xs"
                    leftIcon={
                      isConnected ? (
                        <Check className="w-3.5 h-3.5 text-emerald-500" />
                      ) : (
                        <UserPlus className="w-3.5 h-3.5" />
                      )
                    }
                    onClick={() => handleConnectToggle(dev.id)}
                  >
                    {isConnected ? 'Connected' : 'Connect'}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls */}
      {!isLoading && totalPages > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-border">
          <p className="text-xs text-muted">
            Showing <strong className="text-foreground">{page * pageSize + 1}</strong> to{' '}
            <strong className="text-foreground">
              {Math.min((page + 1) * pageSize, totalElements)}
            </strong>{' '}
            of <strong className="text-foreground">{totalElements}</strong> developers
          </p>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => {
                setPage((prev) => Math.max(prev - 1, 0));
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              leftIcon={<ChevronLeft className="w-3.5 h-3.5" />}
              className="text-xs"
            >
              Previous
            </Button>

            <span className="text-xs text-muted font-medium px-2">
              Page <strong className="text-foreground">{page + 1}</strong> of{' '}
              <strong className="text-foreground">{totalPages}</strong>
            </span>

            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => {
                setPage((prev) => prev + 1);
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              rightIcon={<ChevronRight className="w-3.5 h-3.5" />}
              className="text-xs"
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function DevelopersFallback() {
  return (
    <div className="space-y-6">
      <div className="h-14 rounded-lg bg-surface animate-pulse" />
      <div className="h-24 rounded-xl bg-card border border-border animate-pulse" />
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {Array.from({ length: 6 }).map((_, idx) => (
          <div key={idx} className="h-64 rounded-xl bg-card border border-border animate-pulse" />
        ))}
      </div>
    </div>
  );
}

export default function DevelopersPage() {
  return (
    <AppLayout>
      <Suspense fallback={<DevelopersFallback />}>
        <DevelopersContent />
      </Suspense>
    </AppLayout>
  );
}
