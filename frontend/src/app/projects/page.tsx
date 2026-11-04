'use client';

import React, { useState, useEffect, useTransition } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Search,
  Plus,
  FolderKanban,
  X,
  Lock,
  Globe,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Filter,
  ArrowUpDown,
  Edit3,
  Calendar,
  Layers,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { EmptyState } from '@/components/ui/EmptyState';
import { GithubIcon } from '@/components/ui/Icons';
import { useAuth } from '@/context/AuthContext';
import { projectService } from '@/services/project.service';
import {
  Project,
  ProjectSummary,
  ProjectCategory,
  ProjectStatus,
  ProjectSortOption,
  CATEGORY_LABELS,
  STATUS_LABELS,
} from '@/types/project.types';

const CATEGORIES: { value: string; label: string }[] = [
  { value: 'ALL', label: 'All Categories' },
  { value: 'FULL_STACK', label: CATEGORY_LABELS.FULL_STACK },
  { value: 'FRONTEND', label: CATEGORY_LABELS.FRONTEND },
  { value: 'BACKEND', label: CATEGORY_LABELS.BACKEND },
  { value: 'MOBILE', label: CATEGORY_LABELS.MOBILE },
  { value: 'AI_ML', label: CATEGORY_LABELS.AI_ML },
  { value: 'DEVOPS', label: CATEGORY_LABELS.DEVOPS },
  { value: 'BLOCKCHAIN', label: CATEGORY_LABELS.BLOCKCHAIN },
  { value: 'CYBERSECURITY', label: CATEGORY_LABELS.CYBERSECURITY },
  { value: 'OPEN_SOURCE', label: CATEGORY_LABELS.OPEN_SOURCE },
  { value: 'OTHER', label: CATEGORY_LABELS.OTHER },
];

const STATUSES: { value: string; label: string }[] = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'IDEA', label: STATUS_LABELS.IDEA },
  { value: 'PLANNING', label: STATUS_LABELS.PLANNING },
  { value: 'IN_PROGRESS', label: STATUS_LABELS.IN_PROGRESS },
  { value: 'COMPLETED', label: STATUS_LABELS.COMPLETED },
  { value: 'ON_HOLD', label: STATUS_LABELS.ON_HOLD },
  { value: 'ARCHIVED', label: STATUS_LABELS.ARCHIVED },
];

const SORT_OPTIONS: { value: ProjectSortOption; label: string }[] = [
  { value: 'recently_updated', label: 'Recently Updated' },
  { value: 'newest', label: 'Newest First' },
  { value: 'title_asc', label: 'Title (A → Z)' },
  { value: 'title_desc', label: 'Title (Z → A)' },
];

export default function ProjectsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const [, startTransition] = useTransition();

  // Active Tab: 'explore' | 'mine'
  const [activeTab, setActiveTab] = useState<'explore' | 'mine'>('explore');

  // Explore filters & pagination
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedSort, setSelectedSort] = useState<ProjectSortOption>('recently_updated');
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 9;

  // Data state
  const [exploreProjects, setExploreProjects] = useState<ProjectSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isExploreLoading, setIsExploreLoading] = useState(true);

  // My Projects state
  const [myProjects, setMyProjects] = useState<Project[]>([]);
  const [isMineLoading, setIsMineLoading] = useState(false);
  const [mineError, setMineError] = useState<string | null>(null);

  // Debounce search query (350ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery);
      setCurrentPage(0); // Reset to page 0 on search input
      setIsExploreLoading(true);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Load projects data reactively
  useEffect(() => {
    let isMounted = true;

    if (activeTab === 'explore') {
      projectService
        .browseProjects({
          keyword: debouncedQuery.trim() || undefined,
          category: selectedCategory !== 'ALL' ? (selectedCategory as ProjectCategory) : undefined,
          status: selectedStatus !== 'ALL' ? (selectedStatus as ProjectStatus) : undefined,
          page: currentPage,
          size: pageSize,
          sort: selectedSort,
        })
        .then((response) => {
          if (isMounted) {
            setExploreProjects(response.content || []);
            setTotalPages(response.totalPages || 0);
            setTotalElements(response.totalElements || 0);
            setIsExploreLoading(false);
          }
        })
        .catch(() => {
          if (isMounted) {
            setExploreProjects([]);
            setTotalPages(0);
            setTotalElements(0);
            setIsExploreLoading(false);
          }
        });
    } else if (activeTab === 'mine' && isAuthenticated) {
      projectService
        .getMyProjects()
        .then((projects) => {
          if (isMounted) {
            setMyProjects(projects);
            setIsMineLoading(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            setMineError(err instanceof Error ? err.message : 'Failed to fetch your projects');
            setIsMineLoading(false);
          }
        });
    }

    return () => {
      isMounted = false;
    };
  }, [
    activeTab,
    debouncedQuery,
    selectedCategory,
    selectedStatus,
    selectedSort,
    currentPage,
    isAuthenticated,
  ]);

  const handleResetFilters = () => {
    setSearchQuery('');
    setDebouncedQuery('');
    setSelectedCategory('ALL');
    setSelectedStatus('ALL');
    setSelectedSort('recently_updated');
    setCurrentPage(0);
    setIsExploreLoading(true);
  };

  const hasActiveFilters =
    searchQuery.trim() !== '' ||
    selectedCategory !== 'ALL' ||
    selectedStatus !== 'ALL' ||
    selectedSort !== 'recently_updated';

  const getStatusBadgeClass = (status: ProjectStatus) => {
    switch (status) {
      case 'IN_PROGRESS':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
      case 'COMPLETED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'PLANNING':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'IDEA':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      case 'ON_HOLD':
        return 'bg-yellow-500/10 text-yellow-500 border-yellow-500/30';
      case 'ARCHIVED':
        return 'bg-surface text-muted border-border';
      default:
        return 'bg-surface text-muted border-border';
    }
  };

  return (
    <AppLayout>
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        {/* Header with Title and Create Action */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
                Project Hub
              </h1>
              <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-red-500/10 text-[#EF4444] border border-red-500/20">
                Phase 4
              </span>
            </div>
            <p className="text-xs sm:text-sm text-muted mt-1 max-w-2xl">
              Discover active open projects, examine technology stacks, or showcase your own repository to assemble a high-performing developer team.
            </p>
          </div>

          <div className="flex items-center gap-2.5 self-start sm:self-auto">
            <Link href="/projects/create">
              <Button
                variant="primary"
                size="sm"
                leftIcon={<Plus className="w-4 h-4" />}
                className="shadow-lg shadow-red-500/20"
              >
                Create Project
              </Button>
            </Link>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center justify-between gap-4 border-b border-border">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                setIsExploreLoading(true);
                startTransition(() => {
                  setActiveTab('explore');
                });
              }}
              className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition-all ${
                activeTab === 'explore'
                  ? 'border-[#EF4444] text-[#EF4444]'
                  : 'border-transparent text-muted hover:text-foreground hover:border-border'
              }`}
            >
              <Globe className="w-4 h-4" />
              <span>Explore Public Projects</span>
              {activeTab === 'explore' && totalElements > 0 && (
                <span className="text-xs px-2 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] font-mono">
                  {totalElements}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={() => {
                setIsMineLoading(true);
                startTransition(() => {
                  setActiveTab('mine');
                });
              }}
              className={`flex items-center gap-2 py-3 px-4 text-sm font-semibold border-b-2 transition-all ${
                activeTab === 'mine'
                  ? 'border-[#EF4444] text-[#EF4444]'
                  : 'border-transparent text-muted hover:text-foreground hover:border-border'
              }`}
            >
              <Layers className="w-4 h-4" />
              <span>My Projects</span>
              {isAuthenticated && myProjects.length > 0 && (
                <span className="text-xs px-2 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] font-mono">
                  {myProjects.length}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* ===================== TAB 1: EXPLORE PUBLIC PROJECTS ===================== */}
        {activeTab === 'explore' && (
          <div className="space-y-6">
            {/* Filter Card */}
            <div className="p-4 sm:p-5 rounded-2xl bg-card border border-border shadow-sm space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
                {/* Search query input */}
                <div className="md:col-span-5">
                  <Input
                    placeholder="Search by title, description, or stack..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    leftIcon={<Search className="w-4 h-4 text-muted" />}
                    rightIcon={
                      searchQuery ? (
                        <button
                          onClick={() => setSearchQuery('')}
                          className="p-1 text-muted hover:text-foreground"
                          title="Clear search"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      ) : null
                    }
                  />
                </div>

                {/* Category selector */}
                <div className="md:col-span-3">
                  <div className="relative">
                    <select
                      value={selectedCategory}
                      onChange={(e) => {
                        setSelectedCategory(e.target.value);
                        setCurrentPage(0);
                        setIsExploreLoading(true);
                      }}
                      className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] appearance-none cursor-pointer"
                      aria-label="Filter by category"
                    >
                      {CATEGORIES.map((cat) => (
                        <option key={cat.value} value={cat.value}>
                          {cat.label}
                        </option>
                      ))}
                    </select>
                    <Filter className="w-3.5 h-3.5 text-muted absolute right-3 top-3.5 pointer-events-none" />
                  </div>
                </div>

                {/* Status selector */}
                <div className="md:col-span-2">
                  <div className="relative">
                    <select
                      value={selectedStatus}
                      onChange={(e) => {
                        setSelectedStatus(e.target.value);
                        setCurrentPage(0);
                        setIsExploreLoading(true);
                      }}
                      className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] appearance-none cursor-pointer"
                      aria-label="Filter by status"
                    >
                      {STATUSES.map((stat) => (
                        <option key={stat.value} value={stat.value}>
                          {stat.label}
                        </option>
                      ))}
                    </select>
                    <Filter className="w-3.5 h-3.5 text-muted absolute right-3 top-3.5 pointer-events-none" />
                  </div>
                </div>

                {/* Sort selector */}
                <div className="md:col-span-2">
                  <div className="relative">
                    <select
                      value={selectedSort}
                      onChange={(e) => {
                        setSelectedSort(e.target.value as ProjectSortOption);
                        setCurrentPage(0);
                        setIsExploreLoading(true);
                      }}
                      className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] appearance-none cursor-pointer"
                      aria-label="Sort projects"
                    >
                      {SORT_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                    <ArrowUpDown className="w-3.5 h-3.5 text-muted absolute right-3 top-3.5 pointer-events-none" />
                  </div>
                </div>
              </div>

              {/* Active Filter Chips */}
              {hasActiveFilters && (
                <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-border/60 text-xs">
                  <span className="text-muted font-medium">Active Filters:</span>
                  {debouncedQuery && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/20">
                      Query: &ldquo;{debouncedQuery}&rdquo;
                      <button
                        onClick={() => setSearchQuery('')}
                        className="hover:opacity-75 ml-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  )}
                  {selectedCategory !== 'ALL' && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/20">
                      {CATEGORY_LABELS[selectedCategory as ProjectCategory]}
                      <button
                        onClick={() => {
                          setSelectedCategory('ALL');
                          setIsExploreLoading(true);
                        }}
                        className="hover:opacity-75 ml-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  )}
                  {selectedStatus !== 'ALL' && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-surface text-foreground border border-border">
                      {STATUS_LABELS[selectedStatus as ProjectStatus]}
                      <button
                        onClick={() => {
                          setSelectedStatus('ALL');
                          setIsExploreLoading(true);
                        }}
                        className="hover:opacity-75 ml-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  )}
                  <button
                    onClick={handleResetFilters}
                    className="text-muted hover:text-[#EF4444] underline font-medium ml-auto"
                  >
                    Reset all filters
                  </button>
                </div>
              )}
            </div>

            {/* Loading Skeletons */}
            {isExploreLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3, 4, 5, 6].map((idx) => (
                  <div
                    key={idx}
                    className="p-6 rounded-2xl bg-card border border-border space-y-4 animate-pulse"
                  >
                    <div className="flex items-center justify-between">
                      <div className="h-5 w-24 bg-surface rounded-full" />
                      <div className="h-4 w-16 bg-surface rounded-full" />
                    </div>
                    <div className="h-6 w-3/4 bg-surface rounded-md" />
                    <div className="space-y-2">
                      <div className="h-3 w-full bg-surface rounded" />
                      <div className="h-3 w-5/6 bg-surface rounded" />
                    </div>
                    <div className="flex gap-2 pt-2">
                      <div className="h-5 w-14 bg-surface rounded-md" />
                      <div className="h-5 w-16 bg-surface rounded-md" />
                      <div className="h-5 w-12 bg-surface rounded-md" />
                    </div>
                    <div className="pt-4 border-t border-border flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-surface" />
                        <div className="h-3 w-20 bg-surface rounded" />
                      </div>
                      <div className="h-7 w-24 bg-surface rounded-lg" />
                    </div>
                  </div>
                ))}
              </div>
            ) : exploreProjects.length === 0 ? (
              <EmptyState
                icon={<FolderKanban className="w-8 h-8 text-muted" />}
                title="No projects matched your criteria"
                description={
                  hasActiveFilters
                    ? 'Try clearing some filters or searching for different keywords.'
                    : 'Be the first developer to publish a project to BuildSync!'
                }
                actionLabel={hasActiveFilters ? 'Reset Filters' : 'Create Project'}
                onAction={hasActiveFilters ? handleResetFilters : () => router.push('/projects/create')}
              />
            ) : (
              <>
                {/* Project Cards Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {exploreProjects.map((project) => (
                    <div
                      key={project.id}
                      className="p-6 rounded-2xl bg-card border border-border flex flex-col justify-between space-y-5 hover:border-red-500/40 hover:shadow-lg hover:shadow-red-500/5 transition-all duration-200 group relative"
                    >
                      <div className="space-y-3">
                        {/* Top Meta Bar */}
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/25">
                            {CATEGORY_LABELS[project.category] || project.category}
                          </span>
                          <span
                            className={`text-[11px] font-semibold px-2 py-0.5 rounded-full border ${getStatusBadgeClass(
                              project.status
                            )}`}
                          >
                            {STATUS_LABELS[project.status] || project.status}
                          </span>
                        </div>

                        {/* Title */}
                        <Link href={`/projects/${project.slug || project.id}`}>
                          <h3 className="text-base font-bold text-foreground group-hover:text-[#EF4444] transition-colors leading-snug line-clamp-1">
                            {project.title}
                          </h3>
                        </Link>

                        {/* Description */}
                        <p className="text-xs text-muted leading-relaxed line-clamp-3">
                          {project.shortDescription}
                        </p>

                        {/* Technologies */}
                        {project.technologies && project.technologies.length > 0 && (
                          <div className="flex flex-wrap gap-1.5 pt-1">
                            {project.technologies.slice(0, 4).map((tech) => (
                              <span
                                key={tech.id}
                                className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-surface text-muted-foreground border border-border"
                              >
                                {tech.name}
                              </span>
                            ))}
                            {project.technologies.length > 4 && (
                              <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-md bg-surface text-muted border border-border">
                                +{project.technologies.length - 4}
                              </span>
                            )}
                          </div>
                        )}
                      </div>

                      {/* Card Footer */}
                      <div className="pt-4 border-t border-border flex items-center justify-between gap-3">
                        {/* Owner */}
                        <Link
                          href={`/developers/${project.owner.username}`}
                          className="flex items-center gap-2 hover:opacity-80 transition-opacity min-w-0"
                        >
                          <Avatar
                            src={project.owner.avatarUrl || undefined}
                            name={project.owner.fullName || project.owner.username}
                            size="xs"
                          />
                          <div className="truncate">
                            <span className="text-xs font-medium text-foreground block truncate">
                              {project.owner.fullName || project.owner.username}
                            </span>
                            <span className="text-[10px] text-muted block truncate">
                              @{project.owner.username}
                            </span>
                          </div>
                        </Link>

                        {/* Action buttons */}
                        <div className="flex items-center gap-1.5 shrink-0">
                          {project.githubUrl && (
                            <a
                              href={project.githubUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 rounded-lg border border-border bg-surface text-muted hover:text-foreground transition-colors"
                              title="GitHub Repository"
                            >
                              <GithubIcon className="w-3.5 h-3.5" />
                            </a>
                          )}
                          {project.demoUrl && (
                            <a
                              href={project.demoUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 rounded-lg border border-border bg-surface text-muted hover:text-foreground transition-colors"
                              title="Live Demo"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                            </a>
                          )}
                          <Link href={`/projects/${project.slug || project.id}`}>
                            <Button variant="outline" size="sm" className="text-xs h-8 px-2.5">
                              View
                            </Button>
                          </Link>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-border">
                    <p className="text-xs text-muted">
                      Showing{' '}
                      <span className="font-semibold text-foreground">
                        {currentPage * pageSize + 1}
                      </span>{' '}
                      to{' '}
                      <span className="font-semibold text-foreground">
                        {Math.min((currentPage + 1) * pageSize, totalElements)}
                      </span>{' '}
                      of <span className="font-semibold text-foreground">{totalElements}</span>{' '}
                      projects
                    </p>

                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={currentPage === 0}
                        onClick={() => {
                          setCurrentPage((p) => Math.max(0, p - 1));
                          setIsExploreLoading(true);
                        }}
                        leftIcon={<ChevronLeft className="w-3.5 h-3.5" />}
                        className="text-xs"
                      >
                        Previous
                      </Button>
                      <span className="text-xs font-mono text-muted px-2">
                        {currentPage + 1} / {totalPages}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={currentPage >= totalPages - 1}
                        onClick={() => {
                          setCurrentPage((p) => Math.min(totalPages - 1, p + 1));
                          setIsExploreLoading(true);
                        }}
                        rightIcon={<ChevronRight className="w-3.5 h-3.5" />}
                        className="text-xs"
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* ===================== TAB 2: MY PROJECTS ===================== */}
        {activeTab === 'mine' && (
          <div className="space-y-6">
            {!isAuthenticated ? (
              <EmptyState
                icon={<Lock className="w-8 h-8 text-muted" />}
                title="Authentication Required"
                description="Sign in or create an account to view and manage your repositories and projects."
                actionLabel="Sign In"
                onAction={() => router.push('/login')}
              />
            ) : isMineLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {[1, 2, 3].map((idx) => (
                  <div
                    key={idx}
                    className="p-6 rounded-2xl bg-card border border-border space-y-4 animate-pulse"
                  >
                    <div className="h-5 w-24 bg-surface rounded-full" />
                    <div className="h-6 w-3/4 bg-surface rounded-md" />
                    <div className="h-12 w-full bg-surface rounded" />
                  </div>
                ))}
              </div>
            ) : mineError ? (
              <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-500 text-xs">
                {mineError}
              </div>
            ) : myProjects.length === 0 ? (
              <EmptyState
                icon={<FolderKanban className="w-8 h-8 text-muted" />}
                title="You haven't created any projects yet"
                description="Share your ideas, open-source repositories, or commercial projects to recruit collaborators."
                actionLabel="Create Your First Project"
                onAction={() => router.push('/projects/create')}
              />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {myProjects.map((project) => (
                  <div
                    key={project.id}
                    className="p-6 rounded-2xl bg-card border border-border flex flex-col justify-between space-y-5 hover:border-red-500/40 hover:shadow-lg hover:shadow-red-500/5 transition-all duration-200 group"
                  >
                    <div className="space-y-3">
                      {/* Top Meta Bar */}
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/25">
                          {CATEGORY_LABELS[project.category] || project.category}
                        </span>
                        <div className="flex items-center gap-1.5">
                          {project.visibility === 'PRIVATE' ? (
                            <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-surface text-muted border border-border">
                              <Lock className="w-2.5 h-2.5" /> Private
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              <Globe className="w-2.5 h-2.5" /> Public
                            </span>
                          )}
                          <span
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${getStatusBadgeClass(
                              project.status
                            )}`}
                          >
                            {STATUS_LABELS[project.status] || project.status}
                          </span>
                        </div>
                      </div>

                      {/* Title */}
                      <Link href={`/projects/${project.slug || project.id}`}>
                        <h3 className="text-base font-bold text-foreground group-hover:text-[#EF4444] transition-colors leading-snug line-clamp-1">
                          {project.title}
                        </h3>
                      </Link>

                      {/* Description */}
                      <p className="text-xs text-muted leading-relaxed line-clamp-3">
                        {project.shortDescription}
                      </p>

                      {/* Tech stack */}
                      {project.technologies && project.technologies.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 pt-1">
                          {project.technologies.slice(0, 4).map((tech) => (
                            <span
                              key={tech.id}
                              className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-surface text-muted-foreground border border-border"
                            >
                              {tech.name}
                            </span>
                          ))}
                          {project.technologies.length > 4 && (
                            <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-md bg-surface text-muted border border-border">
                              +{project.technologies.length - 4}
                            </span>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Card Footer: Dates + Actions */}
                    <div className="pt-4 border-t border-border flex items-center justify-between gap-3">
                      <div className="flex items-center gap-1.5 text-[11px] text-muted">
                        <Calendar className="w-3.5 h-3.5" />
                        <span>
                          {project.startDate
                            ? new Date(project.startDate).toLocaleDateString('en-US', {
                                month: 'short',
                                year: 'numeric',
                              })
                            : 'No start date'}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <Link href={`/projects/${project.id}/edit`}>
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Edit3 className="w-3.5 h-3.5" />}
                            className="text-xs h-8 px-2.5"
                          >
                            Edit
                          </Button>
                        </Link>
                        <Link href={`/projects/${project.slug || project.id}`}>
                          <Button variant="ghost" size="sm" className="text-xs h-8 px-2.5">
                            Details
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
