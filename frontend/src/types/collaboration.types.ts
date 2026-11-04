/**
 * BuildSync Collaboration & Team Management Types
 */

export type ProjectMemberRole = 'MEMBER' | 'CONTRIBUTOR' | 'MAINTAINER';

export type CollaborationRequestType = 'JOIN_REQUEST' | 'INVITATION';

export type CollaborationRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

export interface CollaborationUserSummary {
  id: string;
  username: string;
  fullName: string;
  avatarUrl?: string | null;
  headline?: string | null;
}

export interface CollaborationProjectSummary {
  id: string;
  title: string;
  slug: string;
  shortDescription?: string | null;
  status?: string | null;
  category?: string | null;
  ownerUsername?: string | null;
}

export interface ProjectMemberResponse {
  id: string;
  projectId: string;
  user: CollaborationUserSummary;
  role: ProjectMemberRole;
  joinedAt: string;
}

export interface CollaborationRequestResponse {
  id: string;
  type: CollaborationRequestType;
  status: CollaborationRequestStatus;
  message?: string | null;
  project: CollaborationProjectSummary;
  requester: CollaborationUserSummary;
  recipient: CollaborationUserSummary;
  createdAt: string;
  updatedAt: string;
}

export interface UserTeamResponse {
  projectId: string;
  title: string;
  slug: string;
  category?: string | null;
  status?: string | null;
  role: string;
  isOwner: boolean;
  joinedAt: string;
  memberCount: number;
  owner: CollaborationUserSummary;
}

export interface CreateJoinRequestPayload {
  message?: string;
}

export interface CreateInvitationPayload {
  inviteeUsername: string;
  message?: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

