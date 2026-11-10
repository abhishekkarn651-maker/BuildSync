import { API_CONFIG } from '@/config/api.config';
import { apiClient } from './api-client';
import {
  ConversationDetail,
  ConversationParticipantSummary,
  ConversationSummary,
  ConversationType,
  CreateDirectConversationRequest,
  MessageItem,
  MessageSenderSummary,
  MessageType,
  PagedResponse,
  SendMessagePayload,
  UnreadCountResponse,
} from '@/types/messaging.types';

export interface RawParticipant {
  id?: string;
  userId?: string;
  username?: string;
  fullName?: string;
  avatarUrl?: string | null;
  joinedAt?: string;
  lastReadAt?: string | null;
}

export interface RawSender {
  id?: string;
  userId?: string;
  username?: string;
  fullName?: string;
  avatarUrl?: string | null;
  headline?: string | null;
}

export interface RawProject {
  id: string;
  title?: string;
  slug?: string;
  visibility?: string;
  category?: string;
}

export interface RawLastMessage {
  id: string;
  content?: string;
  senderUsername?: string;
  createdAt?: string;
  messageType?: MessageType;
}

export interface RawConversationSummary {
  id: string;
  type?: ConversationType;
  conversationType?: ConversationType;
  name?: string;
  directParticipant?: RawParticipant | null;
  otherParticipant?: RawParticipant | null;
  project?: RawProject | null;
  lastMessage?: RawLastMessage | null;
  unreadCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface RawConversationDetail {
  id: string;
  type?: ConversationType;
  conversationType?: ConversationType;
  name?: string;
  project?: RawProject | null;
  participants?: RawParticipant[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RawMessageItem {
  id: string;
  conversationId: string;
  sender?: RawSender;
  content?: string;
  messageType?: MessageType;
  createdAt?: string;
  updatedAt?: string;
  optimistic?: boolean;
  failed?: boolean;
}

export function normalizeParticipant(raw?: RawParticipant | null): ConversationParticipantSummary {
  if (!raw) {
    return {
      userId: '',
      username: 'User',
      fullName: 'User',
      avatarUrl: null,
    };
  }
  const uid = raw.userId || raw.id || '';
  return {
    id: raw.id || uid,
    userId: uid,
    username: raw.username || 'User',
    fullName: raw.fullName || raw.username || 'User',
    avatarUrl: raw.avatarUrl || null,
    joinedAt: raw.joinedAt,
    lastReadAt: raw.lastReadAt,
  };
}

export function normalizeSender(raw?: RawSender | null): MessageSenderSummary {
  if (!raw) {
    return {
      userId: '',
      username: 'User',
      fullName: 'User',
      avatarUrl: null,
    };
  }
  const uid = raw.userId || raw.id || '';
  return {
    id: raw.id || uid,
    userId: uid,
    username: raw.username || 'User',
    fullName: raw.fullName || raw.username || 'User',
    avatarUrl: raw.avatarUrl || null,
    headline: raw.headline || null,
  };
}

export function normalizeConversationSummary(raw: RawConversationSummary): ConversationSummary {
  if (!raw) {
    throw new Error('Invalid conversation summary payload');
  }

  const type: ConversationType = raw.type || raw.conversationType || 'DIRECT';
  const participantRaw = raw.directParticipant || raw.otherParticipant || null;
  const directParticipant = participantRaw ? normalizeParticipant(participantRaw) : null;
  const project = raw.project
    ? {
        id: raw.project.id,
        title: raw.project.title || 'Untitled Project',
        slug: raw.project.slug || '',
        visibility: raw.project.visibility,
        category: raw.project.category,
      }
    : null;

  const defaultName =
    type === 'PROJECT'
      ? project?.title || 'Project Chat'
      : directParticipant?.fullName || directParticipant?.username || 'Developer';

  return {
    id: raw.id,
    type,
    conversationType: type,
    name: raw.name || defaultName,
    directParticipant,
    otherParticipant: directParticipant,
    project,
    lastMessage: raw.lastMessage
      ? {
          id: raw.lastMessage.id,
          content: raw.lastMessage.content || '',
          senderUsername: raw.lastMessage.senderUsername || '',
          createdAt: raw.lastMessage.createdAt || new Date().toISOString(),
          messageType: raw.lastMessage.messageType || 'TEXT',
        }
      : null,
    unreadCount: typeof raw.unreadCount === 'number' ? raw.unreadCount : 0,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt || raw.createdAt || new Date().toISOString(),
  };
}

export function normalizeConversationDetail(raw: RawConversationDetail): ConversationDetail {
  if (!raw) {
    throw new Error('Invalid conversation detail payload');
  }

  const type: ConversationType = raw.type || raw.conversationType || 'DIRECT';
  const project = raw.project
    ? {
        id: raw.project.id,
        title: raw.project.title || 'Untitled Project',
        slug: raw.project.slug || '',
        visibility: raw.project.visibility,
        category: raw.project.category,
      }
    : null;

  const participants: ConversationParticipantSummary[] = Array.isArray(raw.participants)
    ? raw.participants.map(normalizeParticipant)
    : [];

  return {
    id: raw.id,
    type,
    conversationType: type,
    name: raw.name || (type === 'PROJECT' ? project?.title || 'Project Chat' : 'Direct Chat'),
    project,
    participants,
    createdAt: raw.createdAt || new Date().toISOString(),
    updatedAt: raw.updatedAt || raw.createdAt || new Date().toISOString(),
  };
}

export function normalizeMessageItem(raw: RawMessageItem): MessageItem {
  if (!raw) {
    throw new Error('Invalid message payload');
  }

  return {
    id: raw.id,
    conversationId: raw.conversationId,
    sender: normalizeSender(raw.sender),
    content: raw.content || '',
    messageType: raw.messageType || 'TEXT',
    createdAt: raw.createdAt || new Date().toISOString(),
    updatedAt: raw.updatedAt,
    optimistic: raw.optimistic,
    failed: raw.failed,
  };
}

export const messagingService = {
  /**
   * Fetch paginated list of conversations for current user.
   */
  async getUserConversations(
    page = 0,
    size = 20
  ): Promise<PagedResponse<ConversationSummary>> {
    const raw = await apiClient<PagedResponse<RawConversationSummary>>(
      API_CONFIG.endpoints.conversations.base(page, size)
    );
    return {
      ...raw,
      content: Array.isArray(raw.content)
        ? raw.content.map(normalizeConversationSummary)
        : [],
    };
  },

  /**
   * Get or create a direct 1-to-1 conversation with another developer by username.
   */
  async getOrCreateDirectConversation(
    username: string
  ): Promise<ConversationSummary> {
    const payload: CreateDirectConversationRequest = { username };
    const raw = await apiClient<RawConversationSummary>(
      API_CONFIG.endpoints.conversations.direct,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
    return normalizeConversationSummary(raw);
  },

  /**
   * Get or create project conversation for project owner / active members.
   */
  async getOrCreateProjectConversation(
    projectId: string
  ): Promise<ConversationSummary> {
    const raw = await apiClient<RawConversationSummary>(
      API_CONFIG.endpoints.conversations.projectConversation(projectId)
    );
    return normalizeConversationSummary(raw);
  },

  /**
   * Get detailed metadata and participants for a conversation.
   */
  async getConversationDetail(
    conversationId: string
  ): Promise<ConversationDetail> {
    const raw = await apiClient<RawConversationDetail>(
      API_CONFIG.endpoints.conversations.byId(conversationId)
    );
    return normalizeConversationDetail(raw);
  },

  /**
   * Get paginated message history for a conversation (chronological order).
   */
  async getMessageHistory(
    conversationId: string,
    page = 0,
    size = 50
  ): Promise<PagedResponse<MessageItem>> {
    const raw = await apiClient<PagedResponse<RawMessageItem>>(
      API_CONFIG.endpoints.conversations.messages(conversationId, page, size)
    );
    return {
      ...raw,
      content: Array.isArray(raw.content)
        ? raw.content.map(normalizeMessageItem)
        : [],
    };
  },

  /**
   * Send a message via REST.
   */
  async sendMessage(
    conversationId: string,
    content: string
  ): Promise<MessageItem> {
    const payload: SendMessagePayload = { content };
    const raw = await apiClient<RawMessageItem>(
      API_CONFIG.endpoints.conversations.send(conversationId),
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
    return normalizeMessageItem(raw);
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
