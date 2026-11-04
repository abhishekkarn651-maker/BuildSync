'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  ArrowLeft,
  Globe,
  Calendar,
  Layers,
  Edit3,
  Trash2,
  FolderKanban,
  AlertTriangle,
  Lock,
  Eye,
  ExternalLink,
  Users,
  UserPlus,
  LogOut,
  UserMinus,
  X,
  Clock,
  CheckCircle2,
  AlertCircle,
  MessageSquare,
  Star,
  GitFork,
  GitBranch,
  Plus,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { GithubIcon } from '@/components/ui/Icons';
import { projectService } from '@/services/project.service';
import { collaborationService } from '@/services/collaboration.service';
import { githubService } from '@/services/github.service';
import { getAvatarUrl } from '@/config/api.config';
import {
  CATEGORY_LABELS,
  Project,
  STATUS_LABELS,
} from '@/types/project.types';
import {
  CollaborationRequestResponse,
  ProjectMemberResponse,
} from '@/types/collaboration.types';
import {
  ProjectGitHubRepositoryResponse,
  GitHubRepositoryResponse,
} from '@/types/github.types';
import { useAuth } from '@/context/AuthContext';

export default function ProjectDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const projectId = params?.projectId as string;

  const [project, setProject] = useState<Project | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Deletion modal state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Collaboration & Team states
  const [members, setMembers] = useState<ProjectMemberResponse[]>([]);
  const [isMembersLoading, setIsMembersLoading] = useState(false);
  const [pendingRequests, setPendingRequests] = useState<CollaborationRequestResponse[]>([]);
  const [isPendingRequestsModalOpen, setIsPendingRequestsModalOpen] = useState(false);

  // Join Request state
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [joinMessage, setJoinMessage] = useState('');
  const [isSubmittingJoin, setIsSubmittingJoin] = useState(false);
  const [hasApplied, setHasApplied] = useState(false);

  // Invite Developer state
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [inviteUsername, setInviteUsername] = useState('');
  const [inviteMessage, setInviteMessage] = useState('');
  const [isSubmittingInvite, setIsSubmittingInvite] = useState(false);

  // Leave Project state
  const [isLeaveModalOpen, setIsLeaveModalOpen] = useState(false);
  const [isLeaving, setIsLeaving] = useState(false);

  // Remove Member state
  const [memberToRemove, setMemberToRemove] = useState<ProjectMemberResponse | null>(null);
  const [isRemovingMember, setIsRemovingMember] = useState(false);

  // GitHub Linked Repositories state
  const [linkedRepos, setLinkedRepos] = useState<ProjectGitHubRepositoryResponse[]>([]);
  const [isLinkedReposLoading, setIsLinkedReposLoading] = useState(false);
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [availableUserRepos, setAvailableUserRepos] = useState<GitHubRepositoryResponse[]>([]);
  const [isLoadingAvailableRepos, setIsLoadingAvailableRepos] = useState(false);
  const [selectedRepoId, setSelectedRepoId] = useState<string>('');
  const [isSubmittingLink, setIsSubmittingLink] = useState(false);
  const [unlinkingRepoId, setUnlinkingRepoId] = useState<string | null>(null);

  // In-app Toast / Notification
  const [toast, setToast] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Load project details
  useEffect(() => {
    let isMounted = true;

    async function loadProject() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await projectService.getProject(projectId);
        if (isMounted) {
          if (data) {
            setProject(data);
          } else {
            setError('The requested project could not be found or you do not have permission to view it.');
          }
        }
      } catch (err: unknown) {
        if (isMounted) {
          console.error('Failed to load project details:', err);
          setError('Failed to retrieve project details. Please verify your connection.');
        }
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    if (projectId) {
      loadProject();
    }

    return () => {
      isMounted = false;
    };
  }, [projectId]);

  // Load Team Members
  const loadMembers = useCallback(async (projId: string) => {
    setIsMembersLoading(true);
    try {
      const data = await collaborationService.getProjectMembers(projId);
      setMembers(data);
    } catch (err: unknown) {
      console.warn('Could not load team members (may be private or unauthorized):', err);
    } finally {
      setIsMembersLoading(false);
    }
  }, []);

  // Load Pending Requests (owner only)
  const loadPendingRequests = useCallback(async (projId: string) => {
    try {
      const data = await collaborationService.getProjectPendingRequests(projId);
      setPendingRequests(data);
    } catch (err: unknown) {
      console.warn('Could not load pending requests:', err);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;
    if (!project?.id) return;

    async function fetchTeamData() {
      setIsMembersLoading(true);
      try {
        const data = await collaborationService.getProjectMembers(project!.id);
        if (isMounted) setMembers(data);
      } catch (err: unknown) {
        console.warn('Could not load team members:', err);
      } finally {
        if (isMounted) setIsMembersLoading(false);
      }

      if (user?.id === project!.owner.id) {
        try {
          const data = await collaborationService.getProjectPendingRequests(project!.id);
          if (isMounted) setPendingRequests(data);
        } catch (err: unknown) {
          console.warn('Could not load pending requests:', err);
        }
      }

      // Load linked GitHub repositories
      setIsLinkedReposLoading(true);
      try {
        const repoData = await githubService.getProjectRepositories(project!.id);
        if (isMounted) setLinkedRepos(repoData);
      } catch (err: unknown) {
        console.warn('Could not load linked GitHub repositories:', err);
      } finally {
        if (isMounted) setIsLinkedReposLoading(false);
      }
    }

    fetchTeamData();

    return () => {
      isMounted = false;
    };
  }, [project, user?.id]);

  const isOwner = user?.id === project?.owner.id;
  const isMember = members.some((m) => m.user.id === user?.id);

  // GitHub Repository Linking Handlers
  const handleOpenLinkModal = async () => {
    setIsLinkModalOpen(true);
    setIsLoadingAvailableRepos(true);
    try {
      const res = await githubService.getRepositories(0, 100);
      // Filter out repos already linked to this project
      const linkedIds = new Set(linkedRepos.map((lr) => lr.repository.id));
      const unlinked = res.content.filter((r) => !linkedIds.has(r.id));
      setAvailableUserRepos(unlinked);
      if (unlinked.length > 0) {
        setSelectedRepoId(unlinked[0].id);
      }
    } catch {
      setToast({
        type: 'error',
        text: 'Could not load your imported GitHub repositories. Please sync from settings.',
      });
    } finally {
      setIsLoadingAvailableRepos(false);
    }
  };

  const handleLinkRepoSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRepoId || !project) return;
    setIsSubmittingLink(true);
    try {
      const linked = await githubService.linkProjectRepository(project.id, selectedRepoId, true);
      setToast({ type: 'success', text: 'GitHub repository linked successfully!' });
      setIsLinkModalOpen(false);
      setLinkedRepos((prev) => [...prev, linked]);
    } catch (err: unknown) {
      setToast({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to link repository.',
      });
    } finally {
      setIsSubmittingLink(false);
    }
  };

  const handleUnlinkRepo = async (repoId: string) => {
    if (!project) return;
    setUnlinkingRepoId(repoId);
    try {
      await githubService.unlinkProjectRepository(project.id, repoId);
      setToast({ type: 'success', text: 'GitHub repository unlinked successfully.' });
      setLinkedRepos((prev) => prev.filter((r) => r.repository.id !== repoId));
    } catch (err: unknown) {
      setToast({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to unlink repository.',
      });
    } finally {
      setUnlinkingRepoId(null);
    }
  };

  // Handle Project Deletion
  const handleDelete = async () => {
    if (!project) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await projectService.deleteProject(project.id);
      setIsDeleteModalOpen(false);
      router.push('/projects');
    } catch (err: unknown) {
      console.error('Failed to delete project:', err);
      setDeleteError('Failed to delete the project. Please try again.');
      setIsDeleting(false);
    }
  };

  // Handle Request to Join
  const handleJoinRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!project) return;
    setIsSubmittingJoin(true);
    try {
      await collaborationService.requestToJoin(project.id, {
        message: joinMessage.trim() || undefined,
      });
      setIsJoinModalOpen(false);
      setHasApplied(true);
      setJoinMessage('');
      setToast({
        type: 'success',
        text: 'Join request sent successfully! The project owner has been notified.',
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to send join request';
      setToast({ type: 'error', text: msg });
    } finally {
      setIsSubmittingJoin(false);
    }
  };

  // Handle Invite Developer
  const handleInviteDeveloper = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!project || !inviteUsername.trim()) return;
    setIsSubmittingInvite(true);
    try {
      await collaborationService.inviteDeveloper(project.id, {
        inviteeUsername: inviteUsername.trim(),
        message: inviteMessage.trim() || undefined,
      });
      setIsInviteModalOpen(false);
      setInviteUsername('');
      setInviteMessage('');
      setToast({
        type: 'success',
        text: `Invitation sent to @${inviteUsername.trim()}!`,
      });
      loadPendingRequests(project.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to send invitation';
      setToast({ type: 'error', text: msg });
    } finally {
      setIsSubmittingInvite(false);
    }
  };

  // Handle Leave Project
  const handleLeaveProject = async () => {
    if (!project) return;
    setIsLeaving(true);
    try {
      await collaborationService.leaveProject(project.id);
      setIsLeaveModalOpen(false);
      setToast({
        type: 'success',
        text: 'You have left the project team.',
      });
      loadMembers(project.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to leave project';
      setToast({ type: 'error', text: msg });
    } finally {
      setIsLeaving(false);
    }
  };

  // Handle Remove Member
  const handleRemoveMember = async () => {
    if (!project || !memberToRemove) return;
    setIsRemovingMember(true);
    try {
      await collaborationService.removeMember(project.id, memberToRemove.user.id);
      setMemberToRemove(null);
      setToast({
        type: 'success',
        text: `@${memberToRemove.user.username} has been removed from the team.`,
      });
      loadMembers(project.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to remove member';
      setToast({ type: 'error', text: msg });
    } finally {
      setIsRemovingMember(false);
    }
  };

  // Handle Owner Accepting Pending Request
  const handleAcceptPending = async (requestId: string) => {
    if (!project) return;
    try {
      await collaborationService.acceptRequest(requestId);
      setToast({
        type: 'success',
        text: 'Request accepted! Team member added to project.',
      });
      loadPendingRequests(project.id);
      loadMembers(project.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to accept request';
      setToast({ type: 'error', text: msg });
    }
  };

  // Handle Owner Rejecting Pending Request
  const handleRejectPending = async (requestId: string) => {
    if (!project) return;
    try {
      await collaborationService.rejectRequest(requestId);
      setToast({
        type: 'success',
        text: 'Request declined.',
      });
      loadPendingRequests(project.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to reject request';
      setToast({ type: 'error', text: msg });
    }
  };

  if (isLoading) {
    return (
      <AppLayout>
        <div className="space-y-6 max-w-5xl mx-auto">
          <div className="h-6 w-32 bg-surface rounded animate-pulse" />
          <div className="h-64 rounded-2xl bg-card border border-border p-8 animate-pulse space-y-4">
            <div className="h-6 w-48 bg-surface rounded" />
            <div className="h-10 w-96 bg-surface rounded" />
            <div className="h-20 w-full bg-surface rounded" />
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 h-72 rounded-2xl bg-card border border-border animate-pulse" />
            <div className="h-72 rounded-2xl bg-card border border-border animate-pulse" />
          </div>
        </div>
      </AppLayout>
    );
  }

  if (error || !project) {
    return (
      <AppLayout>
        <div className="max-w-xl mx-auto my-12 p-8 rounded-2xl bg-card border border-border text-center space-y-4">
          <FolderKanban className="w-12 h-12 text-[#EF4444] mx-auto" />
          <h2 className="text-xl font-bold text-foreground">Project Not Available</h2>
          <p className="text-sm text-muted">
            {error || 'The requested project could not be found or you do not have permission to view it.'}
          </p>
          <div className="pt-2">
            <Link href="/projects">
              <Button variant="outline" size="md">
                Back to Projects
              </Button>
            </Link>
          </div>
        </div>
      </AppLayout>
    );
  }

  const getStatusStyle = (st: string) => {
    switch (st) {
      case 'IN_PROGRESS':
        return 'bg-emerald-500/10 text-emerald-500 border-emerald-500/30';
      case 'PLANNING':
        return 'bg-blue-500/10 text-blue-500 border-blue-500/30';
      case 'COMPLETED':
        return 'bg-purple-500/10 text-purple-500 border-purple-500/30';
      case 'ON_HOLD':
        return 'bg-amber-500/10 text-amber-500 border-amber-500/30';
      case 'ARCHIVED':
        return 'bg-surface text-muted border-border';
      case 'IDEA':
      default:
        return 'bg-yellow-500/10 text-yellow-500 border-yellow-500/30';
    }
  };

  return (
    <AppLayout>
      <div className="space-y-6 max-w-5xl mx-auto">
        {/* Navigation & Controls */}
        <div className="flex items-center justify-between gap-4">
          <Link
            href="/projects"
            className="inline-flex items-center gap-1.5 text-xs text-muted hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Projects</span>
          </Link>

          <div className="flex items-center gap-2">
            {/* Team Chat shortcut for owner and active members */}
            {(isOwner || isMember) && (
              <Link href={`/messages?project=${project.id}`}>
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<MessageSquare className="w-3.5 h-3.5 text-[#EF4444]" />}
                  className="text-xs font-semibold"
                >
                  Team Chat
                </Button>
              </Link>
            )}

            {/* Owner Controls */}
            {isOwner && (
              <>
                <Link href={`/projects/${project.id}/edit`}>
                  <Button variant="outline" size="sm" leftIcon={<Edit3 className="w-3.5 h-3.5" />}>
                    Edit Project
                  </Button>
                </Link>
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<Trash2 className="w-3.5 h-3.5 text-red-500" />}
                  onClick={() => setIsDeleteModalOpen(true)}
                  className="hover:border-red-500/50 hover:bg-red-500/5 text-muted hover:text-red-500"
                >
                  Delete
                </Button>
              </>
            )}

            {/* Member Control: Leave Project */}
            {isMember && !isOwner && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsLeaveModalOpen(true)}
                leftIcon={<LogOut className="w-3.5 h-3.5 text-amber-500" />}
                className="text-xs text-muted hover:text-amber-500"
              >
                Leave Project
              </Button>
            )}

            {/* Authenticated Non-Member: Request to Join */}
            {!isOwner && !isMember && user && project.visibility === 'PUBLIC' && (
              <Button
                variant="primary"
                size="sm"
                disabled={hasApplied}
                onClick={() => setIsJoinModalOpen(true)}
                leftIcon={<UserPlus className="w-3.5 h-3.5" />}
                className="text-xs"
              >
                {hasApplied ? 'Application Sent' : 'Request to Join'}
              </Button>
            )}
          </div>
        </div>

        {/* Global Feedback Banner */}
        {toast && (
          <div
            className={`p-4 rounded-xl flex items-center justify-between gap-3 text-xs border ${
              toast.type === 'success'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-red-500/10 text-red-400 border-red-500/30'
            }`}
          >
            <div className="flex items-center gap-2.5">
              {toast.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-500" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
              )}
              <span>{toast.text}</span>
            </div>
            <button
              type="button"
              onClick={() => setToast(null)}
              className="text-muted hover:text-foreground"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Project Header Hero Card */}
        <div className="p-6 sm:p-8 rounded-2xl bg-card border border-border shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-6">
            <div className="space-y-3.5 max-w-2xl">
              {/* Category, Status & Visibility Badges */}
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-red-500/10 text-[#EF4444] border border-red-500/20">
                  {CATEGORY_LABELS[project.category] || project.category}
                </span>

                <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full border ${getStatusStyle(project.status)}`}>
                  {STATUS_LABELS[project.status] || project.status}
                </span>

                <span
                  className={`text-xs font-medium px-2 py-0.5 rounded-full border flex items-center gap-1 ${
                    project.visibility === 'PUBLIC'
                      ? 'bg-surface text-muted border-border'
                      : 'bg-amber-500/10 text-amber-500 border-amber-500/30'
                  }`}
                >
                  {project.visibility === 'PUBLIC' ? (
                    <>
                      <Eye className="w-3 h-3" />
                      <span>Public</span>
                    </>
                  ) : (
                    <>
                      <Lock className="w-3 h-3" />
                      <span>Private</span>
                    </>
                  )}
                </span>

                <span className="text-xs text-muted flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-muted" />
                  Updated {new Date(project.updatedAt).toLocaleDateString()}
                </span>
              </div>

              {/* Title & Short Description */}
              <div>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
                  {project.title}
                </h1>
                <p className="text-xs text-muted mt-1 font-mono">
                  slug: {project.slug}
                </p>
              </div>

              <p className="text-sm text-foreground/90 leading-relaxed font-medium">
                {project.shortDescription}
              </p>

              {/* Technologies */}
              <div className="flex flex-wrap gap-1.5 pt-1">
                {project.technologies && project.technologies.length > 0 ? (
                  project.technologies.map((tech) => (
                    <span
                      key={tech.id}
                      className="text-xs font-medium px-2.5 py-1 rounded-lg bg-surface text-foreground border border-border"
                    >
                      {tech.name}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-muted italic">No technologies listed</span>
                )}
              </div>
            </div>

            {/* Quick Action Buttons */}
            <div className="flex flex-col gap-2.5 shrink-0 self-stretch sm:self-auto sm:min-w-[180px]">
              {project.demoUrl && (
                <a
                  href={project.demoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full"
                >
                  <Button variant="primary" size="md" className="w-full" leftIcon={<Globe className="w-4 h-4" />}>
                    Live Demo
                  </Button>
                </a>
              )}

              {project.githubUrl && (
                <a
                  href={project.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full"
                >
                  <Button variant="outline" size="md" className="w-full" leftIcon={<GithubIcon className="w-4 h-4" />}>
                    GitHub Repo
                  </Button>
                </a>
              )}
            </div>
          </div>
        </div>

        {/* Content Grid: Detailed Overview, Team Section & Sidebar */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Column: Detailed Overview & Project Team */}
          <div className="lg:col-span-2 space-y-6">
            {/* Project Overview */}
            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 shadow-sm">
              <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
                <FolderKanban className="w-4 h-4 text-[#EF4444]" />
                Project Overview & Architecture
              </h2>

              {project.detailedDescription ? (
                <div className="text-xs sm:text-sm text-muted leading-relaxed whitespace-pre-line space-y-3">
                  {project.detailedDescription}
                </div>
              ) : (
                <p className="text-xs text-muted italic">
                  No detailed description has been provided for this project yet.
                </p>
              )}
            </div>

            {/* Project Team & Collaborators Section */}
            <div className="p-6 rounded-2xl bg-card border border-border space-y-5 shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border pb-4">
                <div>
                  <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
                    <Users className="w-4 h-4 text-[#EF4444]" />
                    Project Team & Contributors
                  </h2>
                  <p className="text-xs text-muted mt-0.5">
                    Developers collaborating on this codebase
                  </p>
                </div>

                {/* Owner Actions: Invite & Pending Requests */}
                {isOwner && (
                  <div className="flex items-center gap-2">
                    {pendingRequests.length > 0 && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setIsPendingRequestsModalOpen(true)}
                        leftIcon={<Clock className="w-3.5 h-3.5 text-amber-500" />}
                        className="text-xs border-amber-500/30 text-amber-400 bg-amber-500/10"
                      >
                        Pending Requests ({pendingRequests.length})
                      </Button>
                    )}
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => setIsInviteModalOpen(true)}
                      leftIcon={<UserPlus className="w-3.5 h-3.5" />}
                      className="text-xs"
                    >
                      Invite Teammate
                    </Button>
                  </div>
                )}
              </div>

              {/* Members List */}
              <div className="space-y-3">
                {/* 1. Project Owner Card */}
                <div className="p-4 rounded-xl bg-surface/50 border border-border flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <Avatar
                      src={getAvatarUrl(project.owner.avatarUrl)}
                      name={project.owner.fullName || project.owner.username}
                      size="sm"
                    />
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Link
                          href={`/developers/${project.owner.username}`}
                          className="font-semibold text-xs sm:text-sm text-foreground hover:text-[#EF4444] transition-colors truncate"
                        >
                          {project.owner.fullName}
                        </Link>
                        <span className="text-[11px] text-muted">@{project.owner.username}</span>
                        <Badge variant="primary" size="sm">
                          Owner
                        </Badge>
                      </div>
                      <p className="text-[11px] text-muted truncate mt-0.5">
                        {project.owner.headline || 'Project Lead'}
                      </p>
                    </div>
                  </div>
                  <span className="text-[11px] text-muted hidden sm:inline">Project Founder</span>
                </div>

                {/* 2. Active Team Members */}
                {isMembersLoading ? (
                  <div className="space-y-2">
                    {[1, 2].map((i) => (
                      <div key={i} className="h-14 rounded-xl bg-surface animate-pulse" />
                    ))}
                  </div>
                ) : members.length === 0 ? (
                  <div className="p-4 rounded-xl bg-surface/20 border border-dashed border-border text-center">
                    <p className="text-xs text-muted">
                      No additional contributors have joined yet.
                    </p>
                  </div>
                ) : (
                  members.map((member) => (
                    <div
                      key={member.id}
                      className="p-4 rounded-xl bg-surface/30 border border-border hover:border-border/80 flex items-center justify-between gap-3 transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <Avatar
                          src={getAvatarUrl(member.user.avatarUrl)}
                          name={member.user.fullName || member.user.username}
                          size="sm"
                        />
                        <div className="min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <Link
                              href={`/developers/${member.user.username}`}
                              className="font-semibold text-xs sm:text-sm text-foreground hover:text-[#EF4444] transition-colors truncate"
                            >
                              {member.user.fullName}
                            </Link>
                            <span className="text-[11px] text-muted">@{member.user.username}</span>
                            <Badge variant="neutral" size="sm">
                              {member.role}
                            </Badge>
                          </div>
                          <p className="text-[11px] text-muted truncate mt-0.5">
                            {member.user.headline || 'Team Contributor'}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 shrink-0">
                        <span className="text-[11px] text-muted hidden sm:inline">
                          Joined {new Date(member.joinedAt).toLocaleDateString(undefined, {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </span>

                        {/* Owner action: Remove Member */}
                        {isOwner && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setMemberToRemove(member)}
                            className="p-1.5 h-7 w-7 text-muted hover:text-red-400 hover:border-red-500/40"
                            title="Remove member"
                          >
                            <UserMinus className="w-3.5 h-3.5" />
                          </Button>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Timeline & Milestones */}
            {(project.startDate || project.completionDate) && (
              <div className="p-6 rounded-2xl bg-card border border-border space-y-3 shadow-sm">
                <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-[#EF4444]" />
                  Project Timeline
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                  {project.startDate && (
                    <div className="p-3 rounded-xl bg-surface border border-border space-y-1">
                      <span className="text-[11px] text-muted font-medium">Start Date</span>
                      <p className="text-xs font-semibold text-foreground">
                        {new Date(project.startDate).toLocaleDateString(undefined, {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                        })}
                      </p>
                    </div>
                  )}
                  {project.completionDate && (
                    <div className="p-3 rounded-xl bg-surface border border-border space-y-1">
                      <span className="text-[11px] text-muted font-medium">Target Completion</span>
                      <p className="text-xs font-semibold text-foreground">
                        {new Date(project.completionDate).toLocaleDateString(undefined, {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                        })}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}
            {/* Connected GitHub Repositories */}
            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 shadow-sm">
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-surface border border-border flex items-center justify-center text-foreground">
                    <GithubIcon className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
                      Connected GitHub Repositories
                      <span className="text-xs text-muted font-normal">
                        ({linkedRepos.length})
                      </span>
                    </h3>
                    <p className="text-[11px] text-muted">
                      Source repositories linked to this project.
                    </p>
                  </div>
                </div>

                {isOwner && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleOpenLinkModal}
                    className="text-xs flex items-center gap-1.5"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Link Repository</span>
                  </Button>
                )}
              </div>

              {isLinkedReposLoading ? (
                <div className="py-6 text-center text-xs text-muted">
                  Loading linked repositories...
                </div>
              ) : linkedRepos.length === 0 ? (
                <div className="p-5 rounded-xl bg-surface/30 border border-dashed border-border text-center space-y-2">
                  <p className="text-xs text-muted">
                    No GitHub repositories linked to this project yet.
                  </p>
                  {isOwner && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={handleOpenLinkModal}
                      className="text-xs text-[#EF4444] hover:bg-red-500/10"
                    >
                      + Link a GitHub Repository
                    </Button>
                  )}
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-3">
                  {linkedRepos.map(({ id: linkId, repository: repo }) => (
                    <div
                      key={linkId}
                      className="p-4 rounded-xl bg-surface/40 border border-border hover:border-border/80 transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3"
                    >
                      <div className="space-y-1 min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <a
                            href={repo.htmlUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-bold text-xs sm:text-sm text-foreground hover:text-[#EF4444] transition-colors inline-flex items-center gap-1 group truncate"
                          >
                            <GitBranch className="w-3.5 h-3.5 text-muted group-hover:text-[#EF4444] shrink-0" />
                            <span>{repo.fullName}</span>
                            <ExternalLink className="w-3 h-3 text-muted opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                          </a>
                          {repo.isPrivate ? (
                            <Badge variant="warning" size="sm" className="text-[10px]">
                              Private
                            </Badge>
                          ) : (
                            <Badge variant="success" size="sm" className="text-[10px]">
                              Public
                            </Badge>
                          )}
                        </div>

                        {repo.description && (
                          <p className="text-xs text-muted line-clamp-1">
                            {repo.description}
                          </p>
                        )}

                        <div className="flex items-center gap-3 text-xs text-muted pt-1">
                          {repo.primaryLanguage && (
                            <div className="flex items-center gap-1">
                              <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                              <span>{repo.primaryLanguage}</span>
                            </div>
                          )}
                          <div className="flex items-center gap-1">
                            <Star className="w-3 h-3 text-amber-400" />
                            <span>{repo.starsCount}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <GitFork className="w-3 h-3 text-muted" />
                            <span>{repo.forksCount}</span>
                          </div>
                          {repo.defaultBranch && (
                            <span className="text-[11px] text-muted/70 font-mono">
                              branch: {repo.defaultBranch}
                            </span>
                          )}
                        </div>
                      </div>

                      {isOwner && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUnlinkRepo(repo.id)}
                          disabled={unlinkingRepoId === repo.id}
                          className="text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 shrink-0"
                          title="Unlink Repository"
                        >
                          <Trash2 className="w-3.5 h-3.5 mr-1" />
                          <span>Unlink</span>
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Sidebar: Owner Info & Technology Overview */}
          <div className="space-y-6">
            {/* Project Creator Card */}
            <div className="p-6 rounded-2xl bg-card border border-border space-y-4 shadow-sm">
              <h3 className="text-sm font-semibold text-foreground">
                Project Owner
              </h3>

              <div className="flex items-center gap-3">
                <Avatar
                  src={getAvatarUrl(project.owner.avatarUrl)}
                  name={project.owner.fullName || project.owner.username}
                  size="lg"
                />
                <div className="min-w-0">
                  <Link
                    href={`/developers/${encodeURIComponent(project.owner.username)}`}
                    className="text-sm font-bold text-foreground hover:text-[#EF4444] transition-colors truncate block"
                  >
                    {project.owner.fullName}
                  </Link>
                  <p className="text-xs text-muted truncate">
                    @{project.owner.username}
                  </p>
                  {project.owner.headline && (
                    <p className="text-[11px] text-muted-foreground truncate mt-0.5">
                      {project.owner.headline}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-2 border-t border-border">
                <Link href={`/developers/${encodeURIComponent(project.owner.username)}`}>
                  <Button variant="outline" size="sm" className="w-full text-xs" rightIcon={<ExternalLink className="w-3 h-3" />}>
                    View Creator Profile
                  </Button>
                </Link>
              </div>
            </div>

            {/* Stack Details Card */}
            <div className="p-6 rounded-2xl bg-card border border-border space-y-3 shadow-sm">
              <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
                <Layers className="w-4 h-4 text-[#EF4444]" />
                Technical Stack
              </h3>
              <p className="text-xs text-muted">
                Skills and technologies utilized across this repository:
              </p>
              <div className="space-y-2 pt-1">
                {project.technologies.map((t) => (
                  <div
                    key={t.id}
                    className="flex items-center justify-between p-2 rounded-lg bg-surface border border-border text-xs"
                  >
                    <span className="font-medium text-foreground">{t.name}</span>
                    <span className="text-[10px] text-muted uppercase tracking-wider">{t.category.replace('_', ' ')}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ================= MODALS ================= */}

      {/* 1. Request to Join Modal */}
      <Modal
        isOpen={isJoinModalOpen}
        onClose={() => setIsJoinModalOpen(false)}
        title="Request to Join Team"
      >
        <form onSubmit={handleJoinRequest} className="space-y-4">
          <p className="text-xs text-muted">
            Send an application to join <strong className="text-foreground">{project.title}</strong>. The project owner will review your developer profile and message.
          </p>

          <div>
            <label className="block text-xs font-medium text-foreground mb-1.5">
              Introductory Message (Optional)
            </label>
            <textarea
              rows={4}
              placeholder="Explain how you can contribute or what skills you bring to this project..."
              value={joinMessage}
              onChange={(e) => setJoinMessage(e.target.value)}
              className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444] resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsJoinModalOpen(false)}
              disabled={isSubmittingJoin}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={isSubmittingJoin}
            >
              {isSubmittingJoin ? 'Sending...' : 'Submit Application'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* 2. Invite Developer Modal */}
      <Modal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        title="Invite Developer to Team"
      >
        <form onSubmit={handleInviteDeveloper} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-foreground mb-1.5">
              Developer Username <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. alexchen"
              value={inviteUsername}
              onChange={(e) => setInviteUsername(e.target.value)}
              className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444]"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-foreground mb-1.5">
              Personalized Note (Optional)
            </label>
            <textarea
              rows={3}
              placeholder="Describe the role or components they would be working on..."
              value={inviteMessage}
              onChange={(e) => setInviteMessage(e.target.value)}
              className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444] resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsInviteModalOpen(false)}
              disabled={isSubmittingInvite}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={isSubmittingInvite}
            >
              {isSubmittingInvite ? 'Inviting...' : 'Send Invitation'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* 3. Pending Requests Review Modal (Owner only) */}
      <Modal
        isOpen={isPendingRequestsModalOpen}
        onClose={() => setIsPendingRequestsModalOpen(false)}
        title={`Pending Join Requests (${pendingRequests.length})`}
      >
        <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-1">
          {pendingRequests.length === 0 ? (
            <p className="text-xs text-muted text-center py-6">
              No pending join requests for this project.
            </p>
          ) : (
            pendingRequests.map((req) => (
              <div
                key={req.id}
                className="p-4 rounded-xl bg-surface/60 border border-border space-y-3"
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5">
                    <Avatar
                      src={getAvatarUrl(req.requester.avatarUrl)}
                      name={req.requester.fullName || req.requester.username}
                      size="sm"
                    />
                    <div>
                      <Link
                        href={`/developers/${req.requester.username}`}
                        className="text-xs font-bold text-foreground hover:text-[#EF4444] transition-colors"
                      >
                        {req.requester.fullName || req.requester.username}
                      </Link>
                      <p className="text-[11px] text-muted">@{req.requester.username}</p>
                    </div>
                  </div>
                  <span className="text-[11px] text-muted">
                    {new Date(req.createdAt).toLocaleDateString()}
                  </span>
                </div>

                {req.message && (
                  <p className="text-xs text-foreground/80 italic bg-card p-2.5 rounded-lg border border-border">
                    &ldquo;{req.message}&rdquo;
                  </p>
                )}

                <div className="flex items-center justify-end gap-2 pt-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleRejectPending(req.id)}
                    className="text-xs text-muted hover:text-red-400"
                  >
                    Decline
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => handleAcceptPending(req.id)}
                    className="text-xs"
                  >
                    Accept Member
                  </Button>
                </div>
              </div>
            ))
          )}
        </div>
      </Modal>

      {/* 4. Leave Project Confirmation Modal */}
      <Modal
        isOpen={isLeaveModalOpen}
        onClose={() => setIsLeaveModalOpen(false)}
        title="Leave Project Team"
      >
        <div className="space-y-4">
          <p className="text-xs text-muted">
            Are you sure you want to leave the team for <strong className="text-foreground">{project.title}</strong>? You will lose contributor access to this project.
          </p>

          <div className="flex items-center justify-end gap-2.5 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsLeaveModalOpen(false)}
              disabled={isLeaving}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleLeaveProject}
              disabled={isLeaving}
              className="bg-amber-600 hover:bg-amber-700 text-white"
            >
              {isLeaving ? 'Leaving...' : 'Confirm Leave'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 5. Remove Member Confirmation Modal */}
      <Modal
        isOpen={!!memberToRemove}
        onClose={() => setMemberToRemove(null)}
        title="Remove Team Member"
      >
        <div className="space-y-4">
          <p className="text-xs text-muted">
            Are you sure you want to remove <strong className="text-foreground">@{memberToRemove?.user.username}</strong> from the project team?
          </p>

          <div className="flex items-center justify-end gap-2.5 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setMemberToRemove(null)}
              disabled={isRemovingMember}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleRemoveMember}
              disabled={isRemovingMember}
              className="bg-red-600 hover:bg-red-700 text-white"
            >
              {isRemovingMember ? 'Removing...' : 'Remove Member'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 5b. Link GitHub Repository Modal */}
      <Modal
        isOpen={isLinkModalOpen}
        onClose={() => setIsLinkModalOpen(false)}
        title="Link GitHub Repository"
      >
        {isLoadingAvailableRepos ? (
          <div className="py-8 text-center text-xs text-muted">
            Loading your imported repositories...
          </div>
        ) : availableUserRepos.length === 0 ? (
          <div className="py-6 text-center space-y-3">
            <p className="text-xs text-muted">
              No unlinked repositories available. Please sync your repositories from settings first.
            </p>
            <Link href="/settings/integrations/github">
              <Button variant="primary" size="sm" className="text-xs">
                Go to GitHub Settings
              </Button>
            </Link>
          </div>
        ) : (
          <form onSubmit={handleLinkRepoSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-foreground mb-1.5">
                Select Imported Repository <span className="text-red-500">*</span>
              </label>
              <select
                value={selectedRepoId}
                onChange={(e) => setSelectedRepoId(e.target.value)}
                className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground focus:outline-none focus:border-[#EF4444]"
              >
                {availableUserRepos.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.fullName} ({r.primaryLanguage || 'Unknown'}, {r.starsCount} ★)
                  </option>
                ))}
              </select>
            </div>

            <p className="text-[11px] text-muted">
              Linking this repository displays its star count, language, and GitHub link on your project page.
            </p>

            <div className="flex items-center justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsLinkModalOpen(false)}
                disabled={isSubmittingLink}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="sm"
                disabled={isSubmittingLink}
              >
                {isSubmittingLink ? 'Linking...' : 'Link Repository'}
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* 6. Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        title="Delete Project"
      >
        <div className="space-y-4">
          <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 flex items-start gap-3 text-xs text-red-500">
            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">This action cannot be undone.</p>
              <p className="mt-1 text-red-500/90">
                Are you sure you want to delete <strong className="text-foreground">{project.title}</strong>? All team memberships and project records will be permanently removed.
              </p>
            </div>
          </div>

          {deleteError && (
            <p className="text-xs text-red-500 font-medium">{deleteError}</p>
          )}

          <div className="flex items-center justify-end gap-3 pt-3">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsDeleteModalOpen(false)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleDelete}
              disabled={isDeleting}
              className="bg-red-600 hover:bg-red-700 text-white"
            >
              {isDeleting ? 'Deleting...' : 'Permanently Delete'}
            </Button>
          </div>
        </div>
      </Modal>
    </AppLayout>
  );
}
