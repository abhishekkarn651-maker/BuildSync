import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import {
  ConversationDetail,
  ConversationSummary,
  CreateDirectConversationRequest,
  MessageItem,
  PagedResponse,
  SendMessagePayload,
  UnreadCountResponse,
} from '@/types/messaging.types';

export const messagingService = {
  /**
   * Fetch paginated list of conversations for current user.
   */
  async getUserConversations(
    page = 0,
    size = 20
  ): Promise<PagedResponse<ConversationSummary>> {
    return apiClient<PagedResponse<ConversationSummary>>(
      API_CONFIG.endpoints.conversations.base(page, size)
    );
  },

  /**
   * Get or create a direct 1-to-1 conversation with another developer by username.
   */
  async getOrCreateDirectConversation(
    username: string
  ): Promise<ConversationSummary> {
    const payload: CreateDirectConversationRequest = { username };
    return apiClient<ConversationSummary>(
      API_CONFIG.endpoints.conversations.direct,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Get or create project conversation for project owner / active members.
   */
  async getOrCreateProjectConversation(
    projectId: string
  ): Promise<ConversationSummary> {
    return apiClient<ConversationSummary>(
      API_CONFIG.endpoints.conversations.projectConversation(projectId)
    );
  },

  /**
   * Get detailed metadata and participants for a conversation.
   */
  async getConversationDetail(
    conversationId: string
  ): Promise<ConversationDetail> {
    return apiClient<ConversationDetail>(
      API_CONFIG.endpoints.conversations.byId(conversationId)
    );
  },

  /**
   * Get paginated message history for a conversation (chronological order).
   */
  async getMessageHistory(
    conversationId: string,
    page = 0,
    size = 50
  ): Promise<PagedResponse<MessageItem>> {
    return apiClient<PagedResponse<MessageItem>>(
      API_CONFIG.endpoints.conversations.messages(conversationId, page, size)
    );
  },

  /**
   * Send a message via REST.
   */
  async sendMessage(
    conversationId: string,
    content: string
  ): Promise<MessageItem> {
    const payload: SendMessagePayload = { content };
    return apiClient<MessageItem>(
      API_CONFIG.endpoints.conversations.send(conversationId),
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Mark a conversation as read.
   */
  async markAsRead(conversationId: string): Promise<void> {
    await apiClient<void>(
      API_CONFIG.endpoints.conversations.read(conversationId),
      {
        method: 'POST',
      }
    );
  },

  /**
   * Get user's total unread messages count.
   */
  async getUnreadCount(): Promise<UnreadCountResponse> {
    return apiClient<UnreadCountResponse>(
      API_CONFIG.endpoints.conversations.unreadCount
    );
  },
};

