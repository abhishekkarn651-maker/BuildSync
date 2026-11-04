'use client';

import React, { Suspense, useState, useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { Modal } from '@/components/ui/Modal';
import { GithubIcon } from '@/components/ui/Icons';
import {
  GitBranch,
  Star,
  GitFork,
  ExternalLink,
  RefreshCw,
  Unlink,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  Lock,
  Globe,
  Search,
  Code,
  Sparkles,
  ArrowRight,
} from 'lucide-react';
import { githubService } from '@/services/github.service';
import {
  GitHubConnectionStatusResponse,
  GitHubRepositoryResponse,
  PaginatedGitHubRepositories,
} from '@/types/github.types';

function GitHubSettingsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  // Status & Connection State
  const [status, setStatus] = useState<GitHubConnectionStatusResponse | null>(null);
  const [isLoadingStatus, setIsLoadingStatus] = useState(true);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);
  const [isDisconnecting, setIsDisconnecting] = useState(false);

  // Repositories State
  const [repositories, setRepositories] = useState<GitHubRepositoryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);

  // Filters State
  const [search, setSearch] = useState('');
  const [language, setLanguage] = useState('');
  const [visibility, setVisibility] = useState<'all' | 'public' | 'private'>('all');
  const [sortBy, setSortBy] = useState('pushedAt');

  // Feedback Notifications
  const [feedback, setFeedback] = useState<{
    type: 'success' | 'error' | 'info';
    message: string;
  } | null>(() => {
    const callbackStatus = searchParams.get('status');
    const githubUser = searchParams.get('github_user');
    const errorParam = searchParams.get('error');

    if (callbackStatus === 'success' && githubUser) {
      return {
        type: 'success',
        message: `Successfully connected GitHub account @${githubUser}! Your repositories can now be synchronized.`,
      };
    }
    if (callbackStatus === 'error' || errorParam) {
      let msg = 'Failed to connect GitHub account.';
      if (errorParam === 'github_account_already_linked') {
        msg = 'This GitHub account is already linked to another BuildSync user.';
      } else if (errorParam === 'state_expired' || errorParam === 'invalid_state') {
        msg = 'OAuth session expired or is invalid. Please try connecting again.';
      } else if (errorParam === 'access_denied') {
        msg = 'GitHub authorization was cancelled or denied.';
      }
      return { type: 'error', message: msg };
    }
    return null;
  });
  const [togglingRepoId, setTogglingRepoId] = useState<string | null>(null);

  // Clean query params from URL if present
  useEffect(() => {
    if (searchParams.get('status') || searchParams.get('error')) {
      router.replace('/settings/integrations/github');
    }
  }, [searchParams, router]);

  // Initial load of connection status
  useEffect(() => {
    let isMounted = true;
    async function initStatus() {
      try {
        const data = await githubService.getStatus();
        if (isMounted) setStatus(data);
      } catch {
        if (isMounted) {
          setFeedback({
            type: 'error',
            message: 'Failed to retrieve GitHub connection status. Please try again.',
          });
        }
      } finally {
        if (isMounted) setIsLoadingStatus(false);
      }
    }
    initStatus();
    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch repositories whenever connection is active or filters change
  useEffect(() => {
    if (!status?.connected) return;
    let isMounted = true;
    async function fetchRepos() {
      setIsLoadingRepos(true);
      try {
        const isPrivateParam =
          visibility === 'private' ? true : visibility === 'public' ? false : undefined;
        const data: PaginatedGitHubRepositories = await githubService.getRepositories(
          page,
          12,
          search,
          language,
          isPrivateParam,
          undefined,
          sortBy
        );
        if (isMounted) {
          setRepositories(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        }
      } catch {
        if (isMounted) {
          setFeedback({
            type: 'error',
            message: 'Failed to load repositories. Please try syncing again.',
          });
        }
      } finally {
        if (isMounted) setIsLoadingRepos(false);
      }
    }
    fetchRepos();
    return () => {
      isMounted = false;
    };
  }, [status?.connected, page, search, language, visibility, sortBy]);

  // Initiate Connect
  const handleConnect = async () => {
    try {
      setIsConnecting(true);
      setFeedback(null);
      const res = await githubService.connect();
      if (res.authorizationUrl) {
        window.location.href = res.authorizationUrl;
      }
    } catch (err: unknown) {
      setIsConnecting(false);
      setFeedback({
        type: 'error',
        message:
          err instanceof Error
            ? err.message
            : 'Could not initialize GitHub OAuth connection. Please verify backend configuration.',
      });
    }
  };

  // Synchronize Repositories
  const handleSync = async () => {
    try {
      setIsSyncing(true);
      setFeedback(null);
      const res = await githubService.syncRepositories();
      setFeedback({
        type: 'success',
        message: `Sync completed! Imported ${res.importedCount} new repositories and updated ${res.updatedCount} repositories.`,
      });
      const updatedStatus = await githubService.getStatus();
      setStatus(updatedStatus);
      const isPrivateParam =
        visibility === 'private' ? true : visibility === 'public' ? false : undefined;
      const repoData = await githubService.getRepositories(
        page,
        12,
        search,
        language,
        isPrivateParam,
        undefined,
        sortBy
      );
      setRepositories(repoData.content);
      setTotalPages(repoData.totalPages);
      setTotalElements(repoData.totalElements);
    } catch (err: unknown) {
      setFeedback({
        type: 'error',
        message:
          err instanceof Error ? err.message : 'Repository synchronization failed. Please try again.',
      });
    } finally {
      setIsSyncing(false);
    }
  };

  // Disconnect Account
  const handleDisconnect = async () => {
    try {
      setIsDisconnecting(true);
      await githubService.disconnect();
      setShowDisconnectModal(false);
      setStatus({
        connected: false,
        repositoryCount: 0,
        tokenStatus: 'DISCONNECTED',
      });
      setRepositories([]);
      setFeedback({
        type: 'info',
        message:
          'GitHub account disconnected and encrypted tokens deleted. Imported repository metadata has been removed.',
      });
    } catch (err: unknown) {
      setFeedback({
        type: 'error',
        message:
          err instanceof Error
            ? err.message
            : 'Failed to disconnect GitHub account. Please try again.',
      });
    } finally {
      setIsDisconnecting(false);
    }
  };

  // Toggle Showcase
  const handleToggleShowcase = async (repo: GitHubRepositoryResponse) => {
    if (repo.isPrivate) return;
    const targetShowcase = !repo.isShowcased;

    // Optimistic update
    setTogglingRepoId(repo.id);
    setRepositories((prev) =>
      prev.map((r) => (r.id === repo.id ? { ...r, isShowcased: targetShowcase } : r))
    );

    try {
      await githubService.toggleShowcase(repo.id, targetShowcase);
      setFeedback({
        type: 'success',
        message: targetShowcase
          ? `Repository "${repo.name}" is now showcased on your public developer profile.`
          : `Repository "${repo.name}" removed from showcase.`,
      });
    } catch {
      // Revert optimistic update
      setRepositories((prev) =>
        prev.map((r) => (r.id === repo.id ? { ...r, isShowcased: !targetShowcase } : r))
      );
      setFeedback({
        type: 'error',
        message: 'Could not update showcase visibility. Please try again.',
      });
    } finally {
      setTogglingRepoId(null);
    }
  };

  // Available unique languages in current view
  const availableLanguages = Array.from(
    new Set(repositories.map((r) => r.primaryLanguage).filter(Boolean) as string[])
  ).sort();

  return (
    <div className="space-y-8 max-w-5xl pb-16">
      {/* Header */}
      <div className="border-b border-border pb-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-card border border-border flex items-center justify-center text-foreground">
            <GithubIcon className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground flex items-center gap-2">
              GitHub Integration
              <Badge variant="primary" size="sm" className="text-xs">
                Phase 7
              </Badge>
            </h1>
            <p className="text-xs sm:text-sm text-muted mt-1">
              Connect your GitHub account to showcase open-source projects, display verified stats,
              and link repositories to BuildSync projects.
            </p>
          </div>
        </div>
      </div>

      {/* Notifications / Feedback Alerts */}
      {feedback && (
        <div
          className={`p-4 rounded-xl border flex items-start gap-3 animate-in fade-in ${
            feedback.type === 'success'
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
              : feedback.type === 'error'
              ? 'bg-red-500/10 border-red-500/30 text-red-400'
              : 'bg-blue-500/10 border-blue-500/30 text-blue-400'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          ) : (
            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
          )}
          <div className="text-xs sm:text-sm leading-relaxed flex-1">{feedback.message}</div>
          <button
            onClick={() => setFeedback(null)}
            className="text-xs opacity-70 hover:opacity-100 transition-opacity ml-2"
          >
            &times;
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoadingStatus ? (
        <div className="p-8 rounded-2xl bg-card border border-border flex flex-col items-center justify-center space-y-4">
          <RefreshCw className="w-8 h-8 text-[#EF4444] animate-spin" />
          <p className="text-xs text-muted">Checking GitHub connection status...</p>
        </div>
      ) : !status?.connected ? (
        /* DISCONNECTED STATE */
        <div className="space-y-6">
          <div className="p-8 sm:p-10 rounded-2xl bg-card border border-border space-y-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-80 h-80 bg-red-500/5 rounded-full blur-3xl pointer-events-none" />

            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
              <div className="space-y-2 max-w-xl">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface border border-border text-xs text-muted">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                  <span>PKCE OAuth & AES-256-GCM Encrypted at Rest</span>
                </div>
                <h2 className="text-xl sm:text-2xl font-bold text-foreground">
                  Connect Your GitHub Identity
                </h2>
                <p className="text-xs sm:text-sm text-muted leading-relaxed">
                  Seamlessly import your repositories, showcase your top work on your public
                  developer profile, and link repositories directly into collaborative BuildSync
                  projects.
                </p>
              </div>

              <Button
                variant="primary"
                onClick={handleConnect}
                disabled={isConnecting}
                className="shrink-0 flex items-center gap-2 px-6 py-3 text-sm font-semibold shadow-lg shadow-red-500/20"
              >
                <GithubIcon className="w-4 h-4" />
                {isConnecting ? 'Connecting...' : 'Connect GitHub'}
                <ArrowRight className="w-4 h-4 ml-1" />
              </Button>
            </div>

            {/* Scope / Privacy Clarification Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-border/60">
              <div className="p-4 rounded-xl bg-surface/50 border border-border/80 space-y-2">
                <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>What BuildSync Will Do</span>
                </div>
                <ul className="text-xs text-muted space-y-1.5 list-disc list-inside">
                  <li>Import your public & private repository metadata (stars, language, forks)</li>
                  <li>Let you choose which repositories to showcase on your public developer profile</li>
                  <li>Enable linking GitHub repositories to your BuildSync projects</li>
                </ul>
              </div>

              <div className="p-4 rounded-xl bg-surface/50 border border-border/80 space-y-2">
                <div className="flex items-center gap-2 text-xs font-semibold text-amber-400">
                  <Lock className="w-4 h-4" />
                  <span>What BuildSync Never Does</span>
                </div>
                <ul className="text-xs text-muted space-y-1.5 list-disc list-inside">
                  <li>Never pushes code, creates commits, or modifies files</li>
                  <li>Never creates, renames, or deletes your GitHub repositories</li>
                  <li>Never exposes tokens to the browser or client-side storage</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* CONNECTED STATE */
        <div className="space-y-8">
          {/* Connection Profile Card */}
          <div className="p-6 sm:p-8 rounded-2xl bg-card border border-border flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <Avatar
                src={status.githubAvatarUrl || undefined}
                name={status.githubUsername || 'GitHub User'}
                size="xl"
                className="w-16 h-16 rounded-2xl border-2 border-border object-cover shadow-sm"
              />

              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="text-lg font-bold text-foreground">
                    @{status.githubUsername}
                  </h3>
                  {status.tokenStatus === 'VALID' ? (
                    <Badge variant="success" size="sm" className="text-xs flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                      Active & Synced
                    </Badge>
                  ) : (
                    <Badge variant="warning" size="sm" className="text-xs flex items-center gap-1">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      Re-auth Required
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-4 text-xs text-muted flex-wrap">
                  {status.githubProfileUrl && (
                    <a
                      href={status.githubProfileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 hover:text-[#EF4444] transition-colors"
                    >
                      <span>View GitHub Profile</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  )}
                  {status.lastSyncedAt && (
                    <span>
                      Last synced:{' '}
                      {new Date(status.lastSyncedAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  )}
                  <span>{status.repositoryCount} Repositories Cached</span>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-3 w-full md:w-auto">
              <Button
                variant="outline"
                onClick={handleSync}
                disabled={isSyncing}
                className="flex-1 md:flex-initial flex items-center justify-center gap-2 text-xs"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
                {isSyncing ? 'Syncing...' : 'Sync Repositories'}
              </Button>

              <Button
                variant="outline"
                onClick={() => setShowDisconnectModal(true)}
                className="flex-1 md:flex-initial flex items-center justify-center gap-2 text-xs text-red-400 border-red-500/30 hover:bg-red-500/10"
              >
                <Unlink className="w-3.5 h-3.5" />
                Disconnect
              </Button>
            </div>
          </div>

          {/* Repository Management Section */}
          <div className="space-y-5">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-foreground flex items-center gap-2">
                  <span>Imported Repositories</span>
                  <span className="text-xs text-muted font-normal">
                    ({totalElements} total)
                  </span>
                </h2>
                <p className="text-xs text-muted mt-0.5">
                  Toggle the showcase switch on any public repository to highlight it on your
                  public developer profile.
                </p>
              </div>
            </div>

            {/* Filters Bar */}
            <div className="p-4 rounded-xl bg-card border border-border grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
              {/* Search */}
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-muted absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  placeholder="Search repository..."
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setPage(0);
                  }}
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-lg bg-surface border border-border text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444]"
                />
              </div>

              {/* Language Filter */}
              <div>
                <select
                  value={language}
                  onChange={(e) => {
                    setLanguage(e.target.value);
                    setPage(0);
                  }}
                  className="w-full px-3 py-2 text-xs rounded-lg bg-surface border border-border text-foreground focus:outline-none focus:border-[#EF4444]"
                >
                  <option value="">All Languages</option>
                  {availableLanguages.map((lang) => (
                    <option key={lang} value={lang}>
                      {lang}
                    </option>
                  ))}
                </select>
              </div>

              {/* Visibility Filter */}
              <div>
                <select
                  value={visibility}
                  onChange={(e) => {
                    setVisibility(e.target.value as 'all' | 'public' | 'private');
                    setPage(0);
                  }}
                  className="w-full px-3 py-2 text-xs rounded-lg bg-surface border border-border text-foreground focus:outline-none focus:border-[#EF4444]"
                >
                  <option value="all">All Visibility</option>
                  <option value="public">Public Only</option>
                  <option value="private">Private Only</option>
                </select>
              </div>

              {/* Sorting Filter */}
              <div>
                <select
                  value={sortBy}
                  onChange={(e) => {
                    setSortBy(e.target.value);
                    setPage(0);
                  }}
                  className="w-full px-3 py-2 text-xs rounded-lg bg-surface border border-border text-foreground focus:outline-none focus:border-[#EF4444]"
                >
                  <option value="pushedAt">Recently Pushed</option>
                  <option value="stars">Most Stars</option>
                  <option value="updatedAt">Recently Updated</option>
                  <option value="name">Name (A-Z)</option>
                </select>
              </div>
            </div>

            {/* Repositories Grid */}
            {isLoadingRepos ? (
              <div className="p-12 rounded-2xl bg-card border border-border flex flex-col items-center justify-center space-y-3">
                <RefreshCw className="w-6 h-6 text-[#EF4444] animate-spin" />
                <p className="text-xs text-muted">Loading repositories...</p>
              </div>
            ) : repositories.length === 0 ? (
              <div className="p-12 rounded-2xl bg-card border border-border text-center space-y-3">
                <Code className="w-10 h-10 text-muted mx-auto" />
                <h4 className="text-sm font-semibold text-foreground">No repositories found</h4>
                <p className="text-xs text-muted max-w-sm mx-auto">
                  {status.repositoryCount === 0
                    ? 'No repositories have been imported yet. Click "Sync Repositories" above to import your repositories from GitHub.'
                    : 'No repositories match the selected filters. Try clearing your search or filter options.'}
                </p>
                {status.repositoryCount === 0 && (
                  <Button
                    variant="primary"
                    onClick={handleSync}
                    disabled={isSyncing}
                    className="text-xs mt-2"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isSyncing ? 'animate-spin' : ''}`} />
                    Sync Repositories Now
                  </Button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {repositories.map((repo) => {
                  const isPendingToggle = togglingRepoId === repo.id;

                  return (
                    <div
                      key={repo.id}
                      className={`p-5 rounded-2xl bg-card border transition-all flex flex-col justify-between space-y-4 ${
                        repo.isShowcased
                          ? 'border-red-500/40 shadow-sm shadow-red-500/5'
                          : 'border-border hover:border-border/80'
                      }`}
                    >
                      {/* Repo Header */}
                      <div className="space-y-2">
                        <div className="flex items-start justify-between gap-3">
                          <a
                            href={repo.htmlUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-bold text-sm text-foreground hover:text-[#EF4444] transition-colors inline-flex items-center gap-1.5 group flex-1 break-all"
                          >
                            <GitBranch className="w-4 h-4 text-muted group-hover:text-[#EF4444] shrink-0" />
                            <span>{repo.name}</span>
                            <ExternalLink className="w-3 h-3 text-muted opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                          </a>

                          <div className="flex items-center gap-1.5 shrink-0">
                            {repo.isPrivate ? (
                              <Badge
                                variant="warning"
                                size="sm"
                                className="text-[10px] flex items-center gap-1"
                              >
                                <Lock className="w-2.5 h-2.5" />
                                Private
                              </Badge>
                            ) : (
                              <Badge
                                variant="success"
                                size="sm"
                                className="text-[10px] flex items-center gap-1"
                              >
                                <Globe className="w-2.5 h-2.5" />
                                Public
                              </Badge>
                            )}
                          </div>
                        </div>

                        <p className="text-xs text-muted line-clamp-2 min-h-[32px]">
                          {repo.description || 'No description provided.'}
                        </p>
                      </div>

                      {/* Stats & Meta */}
                      <div className="pt-2 border-t border-border/60 flex items-center justify-between text-xs text-muted flex-wrap gap-2">
                        <div className="flex items-center gap-3">
                          {repo.primaryLanguage && (
                            <div className="flex items-center gap-1">
                              <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                              <span>{repo.primaryLanguage}</span>
                            </div>
                          )}
                          <div className="flex items-center gap-1">
                            <Star className="w-3.5 h-3.5 text-amber-400" />
                            <span>{repo.starsCount}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <GitFork className="w-3.5 h-3.5 text-muted" />
                            <span>{repo.forksCount}</span>
                          </div>
                        </div>

                        {repo.githubPushedAt && (
                          <span className="text-[11px] text-muted/80">
                            Updated {new Date(repo.githubPushedAt).toLocaleDateString()}
                          </span>
                        )}
                      </div>

                      {/* Showcase Toggle Action */}
                      <div className="pt-3 border-t border-border/60 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Sparkles
                            className={`w-3.5 h-3.5 ${
                              repo.isShowcased ? 'text-[#EF4444]' : 'text-muted'
                            }`}
                          />
                          <span className="text-xs font-medium text-foreground">
                            Showcase on Profile
                          </span>
                        </div>

                        {repo.isPrivate ? (
                          <div
                            title="Private repositories cannot be showcased on public developer profiles"
                            className="inline-flex items-center gap-1 text-[11px] text-muted cursor-not-allowed opacity-60"
                          >
                            <Lock className="w-3 h-3" />
                            <span>Public Only</span>
                          </div>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleToggleShowcase(repo)}
                            disabled={isPendingToggle}
                            className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                              repo.isShowcased ? 'bg-[#EF4444]' : 'bg-surface border-border'
                            } ${isPendingToggle ? 'opacity-50' : ''}`}
                            aria-label={`Toggle showcase for ${repo.name}`}
                          >
                            <span
                              className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
                                repo.isShowcased ? 'translate-x-4' : 'translate-x-0'
                              }`}
                            />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 border-t border-border">
                <p className="text-xs text-muted">
                  Page {page + 1} of {totalPages} ({totalElements} repositories)
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0 || isLoadingRepos}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="text-xs"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1 || isLoadingRepos}
                    onClick={() => setPage((p) => p + 1)}
                    className="text-xs"
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Disconnect Confirmation Modal */}
      <Modal
        isOpen={showDisconnectModal}
        onClose={() => setShowDisconnectModal(false)}
        title="Disconnect GitHub Account?"
      >
        <div className="space-y-4">
          <p className="text-xs sm:text-sm text-muted leading-relaxed">
            Are you sure you want to disconnect{' '}
            <strong className="text-foreground">@{status?.githubUsername}</strong>?
          </p>
          <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-xs text-red-400 space-y-1">
            <p className="font-semibold">This action will:</p>
            <ul className="list-disc list-inside space-y-0.5">
              <li>Delete your encrypted GitHub access and refresh tokens</li>
              <li>Revoke the GitHub App grant upstream</li>
              <li>Remove all imported repositories from your account showcase</li>
            </ul>
          </div>
          <div className="flex items-center justify-end gap-3 pt-3">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowDisconnectModal(false)}
              disabled={isDisconnecting}
              className="text-xs"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleDisconnect}
              disabled={isDisconnecting}
              className="text-xs bg-red-600 hover:bg-red-700 text-white"
            >
              {isDisconnecting ? 'Disconnecting...' : 'Yes, Disconnect'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default function GitHubSettingsPage() {
  return (
    <AppLayout requireAuth>
      <Suspense
        fallback={
          <div className="p-12 text-center text-xs text-muted">
            <RefreshCw className="w-6 h-6 text-[#EF4444] animate-spin mx-auto mb-2" />
            Loading GitHub settings...
          </div>
        }
      >
        <GitHubSettingsContent />
      </Suspense>
    </AppLayout>
  );
}
