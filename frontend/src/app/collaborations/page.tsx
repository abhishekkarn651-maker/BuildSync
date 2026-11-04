'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import {
  Users,
  Check,
  X,
  Clock,
  MessageSquare,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  Ban,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Tabs } from '@/components/ui/Tabs';
import { EmptyState } from '@/components/ui/EmptyState';
import { getAvatarUrl } from '@/config/api.config';
import { collaborationService } from '@/services/collaboration.service';
import {
  CollaborationRequestResponse,
  UserTeamResponse,
} from '@/types/collaboration.types';

type CollabTab = 'incoming' | 'outgoing' | 'my_teams';

export default function CollaborationsPage() {
  const [activeTab, setActiveTab] = useState<CollabTab>('incoming');

  // Incoming State
  const [incomingRequests, setIncomingRequests] = useState<CollaborationRequestResponse[]>([]);
  const [incomingPage, setIncomingPage] = useState(0);
  const [incomingTotalPages, setIncomingTotalPages] = useState(1);
  const [isIncomingLoading, setIsIncomingLoading] = useState(true);

  // Outgoing State
  const [outgoingRequests, setOutgoingRequests] = useState<CollaborationRequestResponse[]>([]);
  const [outgoingTotal, setOutgoingTotal] = useState(0);
  const [outgoingPage, setOutgoingPage] = useState(0);
  const [outgoingTotalPages, setOutgoingTotalPages] = useState(1);
  const [isOutgoingLoading, setIsOutgoingLoading] = useState(false);

  // My Teams State
  const [myTeams, setMyTeams] = useState<UserTeamResponse[]>([]);
  const [isTeamsLoading, setIsTeamsLoading] = useState(false);

  // Action processing state
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: 'success' | 'error';
    text: string;
  } | null>(null);

  // Clear feedback after 5s
  useEffect(() => {
    if (feedbackMessage) {
      const timer = setTimeout(() => setFeedbackMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [feedbackMessage]);

  // Load Incoming
  const loadIncoming = useCallback(async (page = 0) => {
    setIsIncomingLoading(true);
    try {
      const res = await collaborationService.getIncomingRequests(page, 10);
      setIncomingRequests(res.content);
      setIncomingTotalPages(res.totalPages);
      setIncomingPage(res.page);
    } catch (err: unknown) {
      console.error('Failed to load incoming requests:', err);
    } finally {
      setIsIncomingLoading(false);
    }
  }, []);

  // Load Outgoing
  const loadOutgoing = useCallback(async (page = 0) => {
    setIsOutgoingLoading(true);
    try {
      const res = await collaborationService.getOutgoingRequests(page, 10);
      setOutgoingRequests(res.content);
      setOutgoingTotal(res.totalElements);
      setOutgoingTotalPages(res.totalPages);
      setOutgoingPage(res.page);
    } catch (err: unknown) {
      console.error('Failed to load outgoing requests:', err);
    } finally {
      setIsOutgoingLoading(false);
    }
  }, []);

  // Load My Teams
  const loadTeams = useCallback(async () => {
    setIsTeamsLoading(true);
    try {
      const res = await collaborationService.getMyTeams();
      setMyTeams(res);
    } catch (err: unknown) {
      console.error('Failed to load my teams:', err);
    } finally {
      setIsTeamsLoading(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    let isMounted = true;
    async function init() {
      try {
        const res = await collaborationService.getIncomingRequests(0, 10);
        if (isMounted) {
          setIncomingRequests(res.content);
          setIncomingTotalPages(res.totalPages);
          setIncomingPage(res.page);
        }
      } catch (err: unknown) {
        console.error('Failed to load incoming requests:', err);
      } finally {
        if (isMounted) {
          setIsIncomingLoading(false);
        }
      }
    }
    init();
    return () => {
      isMounted = false;
    };
  }, []);

  // Tab switch trigger
  const handleTabChange = (tabId: string) => {
    const tab = tabId as CollabTab;
    setActiveTab(tab);
    if (tab === 'incoming') {
      loadIncoming(incomingPage);
    } else if (tab === 'outgoing') {
      loadOutgoing(outgoingPage);
    } else if (tab === 'my_teams') {
      loadTeams();
    }
  };

  // Actions
  const handleAccept = async (requestId: string) => {
    setProcessingId(requestId);
    setFeedbackMessage(null);
    try {
      await collaborationService.acceptRequest(requestId);
      setFeedbackMessage({
        type: 'success',
        text: 'Request accepted successfully! Project membership has been updated.',
      });
      loadIncoming(incomingPage);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to accept request';
      setFeedbackMessage({ type: 'error', text: msg });
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (requestId: string) => {
    setProcessingId(requestId);
    setFeedbackMessage(null);
    try {
      await collaborationService.rejectRequest(requestId);
      setFeedbackMessage({
        type: 'success',
        text: 'Request has been declined.',
      });
      loadIncoming(incomingPage);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to reject request';
      setFeedbackMessage({ type: 'error', text: msg });
    } finally {
      setProcessingId(null);
    }
  };

  const handleCancel = async (requestId: string) => {
    setProcessingId(requestId);
    setFeedbackMessage(null);
    try {
      await collaborationService.cancelRequest(requestId);
      setFeedbackMessage({
        type: 'success',
        text: 'Collaboration request has been cancelled.',
      });
      loadOutgoing(outgoingPage);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to cancel request';
      setFeedbackMessage({ type: 'error', text: msg });
    } finally {
      setProcessingId(null);
    }
  };

  const pendingIncomingCount = incomingRequests.filter((r) => r.status === 'PENDING').length;

  return (
    <AppLayout requireAuth>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-5">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              Collaboration & Teams
            </h1>
            <p className="text-xs sm:text-sm text-muted mt-1">
              Manage incoming team invitations, track sent applications, and oversee active project teams.
            </p>
          </div>
          <Link href="/developers">
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Users className="w-4 h-4" />}
              className="self-start sm:self-auto text-xs"
            >
              Discover Developers
            </Button>
          </Link>
        </div>

        {/* Global Feedback Banner */}
        {feedbackMessage && (
          <div
            className={`p-4 rounded-xl flex items-center justify-between gap-3 text-xs border ${
              feedbackMessage.type === 'success'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-red-500/10 text-red-400 border-red-500/30'
            }`}
          >
            <div className="flex items-center gap-2.5">
              {feedbackMessage.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-500" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
              )}
              <span>{feedbackMessage.text}</span>
            </div>
            <button
              type="button"
              onClick={() => setFeedbackMessage(null)}
              className="text-muted hover:text-foreground text-xs"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Navigation Tabs */}
        <Tabs
          tabs={[
            {
              id: 'incoming',
              label: 'Incoming Requests',
              count: pendingIncomingCount > 0 ? pendingIncomingCount : undefined,
            },
            {
              id: 'outgoing',
              label: 'Outgoing Applications',
              count: outgoingTotal > 0 ? outgoingTotal : undefined,
            },
            {
              id: 'my_teams',
              label: 'My Teams',
              count: myTeams.length > 0 ? myTeams.length : undefined,
            },
          ]}
          activeTab={activeTab}
          onChange={handleTabChange}
        />

        {/* ================= INCOMING REQUESTS TAB ================= */}
        {activeTab === 'incoming' && (
          <div className="space-y-4">
            {isIncomingLoading ? (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="p-6 rounded-2xl bg-card border border-border animate-pulse space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-surface" />
                        <div className="space-y-2">
                          <div className="w-32 h-3.5 bg-surface rounded" />
                          <div className="w-24 h-2.5 bg-surface rounded" />
                        </div>
                      </div>
                      <div className="w-20 h-6 bg-surface rounded-full" />
                    </div>
                    <div className="w-full h-12 bg-surface/50 rounded-xl" />
                  </div>
                ))}
              </div>
            ) : incomingRequests.length === 0 ? (
              <EmptyState
                icon={<Clock className="w-6 h-6" />}
                title="No incoming requests"
                description="When developers invite you to join their projects or apply to join your projects, they will appear here."
                actionLabel="Explore Projects"
                onAction={() => {}}
              />
            ) : (
              <div className="space-y-4">
                {incomingRequests.map((req) => {
                  const isInvitation = req.type === 'INVITATION';
                  const isPending = req.status === 'PENDING';
                  const isBusy = processingId === req.id;

                  return (
                    <div
                      key={req.id}
                      className="p-5 sm:p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
                        <div className="flex items-center gap-3">
                          <Avatar
                            src={getAvatarUrl(req.requester.avatarUrl)}
                            name={req.requester.fullName || req.requester.username}
                            size="md"
                          />
                          <div>
                            <div className="flex items-center gap-2 flex-wrap">
                              <Link
                                href={`/developers/${req.requester.username}`}
                                className="font-semibold text-sm text-foreground hover:text-[#EF4444] transition-colors"
                              >
                                {req.requester.fullName || req.requester.username}
                              </Link>
                              <span className="text-xs text-muted">@{req.requester.username}</span>
                              <Badge
                                variant={isInvitation ? 'primary' : 'neutral'}
                                size="sm"
                              >
                                {isInvitation ? 'Project Invitation' : 'Join Request'}
                              </Badge>
                            </div>
                            <p className="text-xs text-muted mt-0.5 line-clamp-1">
                              {req.requester.headline || 'Developer on BuildSync'}
                            </p>
                          </div>
                        </div>

                        {/* Request Status */}
                        <div className="flex items-center gap-2">
                          <span className="text-xs text-muted flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            {new Date(req.createdAt).toLocaleDateString(undefined, {
                              month: 'short',
                              day: 'numeric',
                              year: 'numeric',
                            })}
                          </span>
                          {!isPending && (
                            <Badge
                              variant={
                                req.status === 'ACCEPTED'
                                  ? 'success'
                                  : req.status === 'REJECTED'
                                  ? 'primary'
                                  : 'subtle'
                              }
                              size="sm"
                            >
                              {req.status}
                            </Badge>
                          )}
                        </div>
                      </div>

                      {/* Project contextual banner */}
                      <div className="flex items-center justify-between p-3 rounded-xl bg-surface/60 border border-border text-xs flex-wrap gap-2">
                        <div className="flex items-center gap-2">
                          <span className="text-muted">
                            {isInvitation ? 'Invited you to collaborate on:' : 'Applied to join:'}
                          </span>
                          <Link
                            href={`/projects/${req.project.slug || req.project.id}`}
                            className="font-semibold text-foreground hover:text-[#EF4444] flex items-center gap-1 transition-colors"
                          >
                            <span>{req.project.title}</span>
                            <ExternalLink className="w-3 h-3 text-muted" />
                          </Link>
                        </div>
                        {req.project.category && (
                          <span className="text-[10px] text-muted uppercase tracking-wider bg-surface px-2 py-0.5 rounded border border-border">
                            {req.project.category}
                          </span>
                        )}
                      </div>

                      {/* Custom note/message */}
                      {req.message && (
                        <div className="p-3.5 rounded-xl bg-surface/30 border border-border/80 flex items-start gap-2.5">
                          <MessageSquare className="w-4 h-4 text-muted shrink-0 mt-0.5" />
                          <p className="text-xs text-foreground/90 italic leading-relaxed">
                            &ldquo;{req.message}&rdquo;
                          </p>
                        </div>
                      )}

                      {/* Action buttons (only when PENDING) */}
                      {isPending && (
                        <div className="flex items-center justify-end gap-2.5 pt-1">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleReject(req.id)}
                            disabled={isBusy}
                            leftIcon={<X className="w-3.5 h-3.5" />}
                            className="text-xs text-muted hover:text-red-400"
                          >
                            Decline
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleAccept(req.id)}
                            disabled={isBusy}
                            leftIcon={<Check className="w-3.5 h-3.5" />}
                            className="text-xs"
                          >
                            {isBusy ? 'Processing...' : 'Accept & Sync'}
                          </Button>
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Pagination */}
                {incomingTotalPages > 1 && (
                  <div className="flex items-center justify-between pt-4 border-t border-border text-xs text-muted">
                    <span>
                      Page {incomingPage + 1} of {incomingTotalPages}
                    </span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={incomingPage <= 0}
                        onClick={() => loadIncoming(incomingPage - 1)}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={incomingPage >= incomingTotalPages - 1}
                        onClick={() => loadIncoming(incomingPage + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* ================= OUTGOING APPLICATIONS TAB ================= */}
        {activeTab === 'outgoing' && (
          <div className="space-y-4">
            {isOutgoingLoading ? (
              <div className="space-y-4">
                {[1, 2].map((i) => (
                  <div
                    key={i}
                    className="p-6 rounded-2xl bg-card border border-border animate-pulse space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <div className="w-40 h-4 bg-surface rounded" />
                      <div className="w-20 h-6 bg-surface rounded-full" />
                    </div>
                    <div className="w-full h-10 bg-surface/50 rounded-xl" />
                  </div>
                ))}
              </div>
            ) : outgoingRequests.length === 0 ? (
              <EmptyState
                icon={<Clock className="w-6 h-6" />}
                title="No outgoing requests"
                description="You haven't sent any collaboration invitations or project join requests yet."
                actionLabel="Browse Open Projects"
                onAction={() => {}}
              />
            ) : (
              <div className="space-y-4">
                {outgoingRequests.map((req) => {
                  const isInvitation = req.type === 'INVITATION';
                  const isPending = req.status === 'PENDING';
                  const isBusy = processingId === req.id;

                  return (
                    <div
                      key={req.id}
                      className="p-5 sm:p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
                        <div>
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-sm text-foreground">
                              {isInvitation ? 'Invitation Sent to' : 'Join Request Sent to'}
                            </span>
                            <Link
                              href={`/developers/${req.recipient.username}`}
                              className="font-semibold text-sm text-[#EF4444] hover:underline"
                            >
                              @{req.recipient.username}
                            </Link>
                            <Badge
                              variant={isInvitation ? 'primary' : 'neutral'}
                              size="sm"
                            >
                              {isInvitation ? 'Team Invite' : 'Join Request'}
                            </Badge>
                          </div>
                          <p className="text-xs text-muted mt-1">
                            For project:{' '}
                            <Link
                              href={`/projects/${req.project.slug || req.project.id}`}
                              className="text-foreground hover:text-[#EF4444] font-medium transition-colors"
                            >
                              {req.project.title}
                            </Link>
                          </p>
                        </div>

                        {/* Status & Date */}
                        <div className="flex items-center gap-3">
                          <span className="text-xs text-muted">
                            {new Date(req.createdAt).toLocaleDateString(undefined, {
                              month: 'short',
                              day: 'numeric',
                              year: 'numeric',
                            })}
                          </span>
                          <Badge
                            variant={
                              req.status === 'PENDING'
                                ? 'warning'
                                : req.status === 'ACCEPTED'
                                ? 'success'
                                : req.status === 'REJECTED'
                                ? 'primary'
                                : 'subtle'
                            }
                            size="sm"
                          >
                            {req.status}
                          </Badge>
                        </div>
                      </div>

                      {/* Note / Message */}
                      {req.message && (
                        <div className="p-3.5 rounded-xl bg-surface/30 border border-border/80 flex items-start gap-2.5">
                          <MessageSquare className="w-4 h-4 text-muted shrink-0 mt-0.5" />
                          <p className="text-xs text-foreground/90 italic leading-relaxed">
                            &ldquo;{req.message}&rdquo;
                          </p>
                        </div>
                      )}

                      {/* Cancel button if pending */}
                      {isPending && (
                        <div className="flex items-center justify-end gap-2 pt-1">
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={isBusy}
                            onClick={() => handleCancel(req.id)}
                            leftIcon={<Ban className="w-3.5 h-3.5 text-red-500" />}
                            className="text-xs text-muted hover:text-red-400"
                          >
                            {isBusy ? 'Cancelling...' : 'Cancel Request'}
                          </Button>
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Pagination */}
                {outgoingTotalPages > 1 && (
                  <div className="flex items-center justify-between pt-4 border-t border-border text-xs text-muted">
                    <span>
                      Page {outgoingPage + 1} of {outgoingTotalPages}
                    </span>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={outgoingPage <= 0}
                        onClick={() => loadOutgoing(outgoingPage - 1)}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={outgoingPage >= outgoingTotalPages - 1}
                        onClick={() => loadOutgoing(outgoingPage + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* ================= MY TEAMS TAB ================= */}
        {activeTab === 'my_teams' && (
          <div className="space-y-4">
            {isTeamsLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[1, 2, 3, 4].map((i) => (
                  <div
                    key={i}
                    className="p-6 rounded-2xl bg-card border border-border animate-pulse space-y-4"
                  >
                    <div className="w-32 h-4 bg-surface rounded" />
                    <div className="w-full h-8 bg-surface/50 rounded" />
                  </div>
                ))}
              </div>
            ) : myTeams.length === 0 ? (
              <EmptyState
                icon={<Users className="w-6 h-6" />}
                title="No active team memberships"
                description="You are not currently part of any project teams. Create your own project or collaborate with other developers."
                actionLabel="Explore Projects"
                onAction={() => {}}
              />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {myTeams.map((team) => (
                  <div
                    key={team.projectId}
                    className="p-5 sm:p-6 rounded-2xl bg-card border border-border space-y-4 hover:border-red-500/30 transition-all flex flex-col justify-between"
                  >
                    <div className="space-y-3">
                      <div className="flex items-center justify-between gap-2">
                        <Link
                          href={`/projects/${team.slug || team.projectId}`}
                          className="font-bold text-base text-foreground hover:text-[#EF4444] transition-colors line-clamp-1"
                        >
                          {team.title}
                        </Link>
                        <Badge
                          variant={team.isOwner ? 'primary' : 'neutral'}
                          size="sm"
                        >
                          {team.role}
                        </Badge>
                      </div>

                      <div className="flex items-center gap-2 flex-wrap">
                        {team.category && (
                          <span className="text-[10px] text-muted uppercase tracking-wider bg-surface px-2 py-0.5 rounded border border-border">
                            {team.category}
                          </span>
                        )}
                        {team.status && (
                          <Badge variant="subtle" size="sm">
                            {team.status}
                          </Badge>
                        )}
                      </div>

                      {/* Owner & Member stats */}
                      <div className="pt-2 border-t border-border flex items-center justify-between text-xs text-muted">
                        <div className="flex items-center gap-2">
                          <Avatar
                            src={getAvatarUrl(team.owner.avatarUrl)}
                            name={team.owner.fullName || team.owner.username}
                            size="xs"
                          />
                          <span>
                            Owner:{' '}
                            <Link
                              href={`/developers/${team.owner.username}`}
                              className="text-foreground hover:text-[#EF4444] font-medium"
                            >
                              @{team.owner.username}
                            </Link>
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5 font-medium text-foreground">
                          <Users className="w-3.5 h-3.5 text-muted" />
                          <span>{team.memberCount} {team.memberCount === 1 ? 'member' : 'members'}</span>
                        </div>
                      </div>
                    </div>

                    <div className="pt-4 border-t border-border flex items-center justify-between text-xs">
                      <span className="text-muted">
                        Joined {new Date(team.joinedAt).toLocaleDateString(undefined, {
                          month: 'short',
                          year: 'numeric',
                        })}
                      </span>
                      <Link href={`/projects/${team.slug || team.projectId}`}>
                        <Button
                          variant="outline"
                          size="sm"
                          rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                          className="text-xs"
                        >
                          View Project Team
                        </Button>
                      </Link>
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
