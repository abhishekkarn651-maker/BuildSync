import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import {
  CollaborationRequestResponse,
  CreateInvitationPayload,
  CreateJoinRequestPayload,
  PagedResponse,
  ProjectMemberResponse,
  UserTeamResponse,
} from '@/types/collaboration.types';

export const collaborationService = {
  /**
   * Fetch incoming collaboration requests (invitations received or join requests for owned projects).
   */
  async getIncomingRequests(
    page = 0,
    size = 10
  ): Promise<PagedResponse<CollaborationRequestResponse>> {
    return apiClient<PagedResponse<CollaborationRequestResponse>>(
      API_CONFIG.endpoints.collaborations.incoming(page, size)
    );
  },

  /**
   * Fetch outgoing collaboration requests sent by the authenticated developer.
   */
  async getOutgoingRequests(
    page = 0,
    size = 10
  ): Promise<PagedResponse<CollaborationRequestResponse>> {
    return apiClient<PagedResponse<CollaborationRequestResponse>>(
      API_CONFIG.endpoints.collaborations.outgoing(page, size)
    );
  },

  /**
   * Fetch all teams where the authenticated user is either the owner or an active member.
   */
  async getMyTeams(): Promise<UserTeamResponse[]> {
    return apiClient<UserTeamResponse[]>(
      API_CONFIG.endpoints.collaborations.myTeams
    );
  },

  /**
   * Accept an incoming collaboration request (join request or invitation).
   */
  async acceptRequest(requestId: string): Promise<CollaborationRequestResponse> {
    return apiClient<CollaborationRequestResponse>(
      API_CONFIG.endpoints.collaborations.accept(requestId),
      {
        method: 'POST',
      }
    );
  },

  /**
   * Reject an incoming collaboration request.
   */
  async rejectRequest(requestId: string): Promise<CollaborationRequestResponse> {
    return apiClient<CollaborationRequestResponse>(
      API_CONFIG.endpoints.collaborations.reject(requestId),
      {
        method: 'POST',
      }
    );
  },

  /**
   * Cancel an outgoing pending request.
   */
  async cancelRequest(requestId: string): Promise<CollaborationRequestResponse> {
    return apiClient<CollaborationRequestResponse>(
      API_CONFIG.endpoints.collaborations.cancel(requestId),
      {
        method: 'POST',
      }
    );
  },

  /**
   * Request to join a public project.
   */
  async requestToJoin(
    projectId: string,
    payload?: CreateJoinRequestPayload
  ): Promise<CollaborationRequestResponse> {
    return apiClient<CollaborationRequestResponse>(
      API_CONFIG.endpoints.projects.joinRequests(projectId),
      {
        method: 'POST',
        body: JSON.stringify(payload || {}),
      }
    );
  },

  /**
   * Invite a developer to join a project (owner only).
   */
  async inviteDeveloper(
    projectId: string,
    payload: CreateInvitationPayload
  ): Promise<CollaborationRequestResponse> {
    return apiClient<CollaborationRequestResponse>(
      API_CONFIG.endpoints.projects.invitations(projectId),
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Get active team members for a project.
   */
  async getProjectMembers(projectId: string): Promise<ProjectMemberResponse[]> {
    return apiClient<ProjectMemberResponse[]>(
      API_CONFIG.endpoints.projects.members(projectId)
    );
  },

  /**
   * Remove a member from a project (owner only).
   */
  async removeMember(projectId: string, userId: string): Promise<void> {
    return apiClient<void>(
      API_CONFIG.endpoints.projects.removeMember(projectId, userId),
      {
        method: 'DELETE',
      }
    );
  },

  /**
   * Leave a project (member only).
   */
  async leaveProject(projectId: string): Promise<void> {
    return apiClient<void>(
      API_CONFIG.endpoints.projects.leave(projectId),
      {
        method: 'POST',
      }
    );
  },

  /**
   * Get pending join requests for a project (owner only).
   */
  async getProjectPendingRequests(
    projectId: string
  ): Promise<CollaborationRequestResponse[]> {
    return apiClient<CollaborationRequestResponse[]>(
      API_CONFIG.endpoints.projects.pendingRequests(projectId)
    );
  },
};

