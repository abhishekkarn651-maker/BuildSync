/**
 * BuildSync Messaging Type Definitions
 */

export type ConversationType = 'DIRECT' | 'PROJECT';
export type MessageType = 'TEXT' | 'SYSTEM';

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ConversationParticipantSummary {
  userId: string;
  username: string;
  fullName: string;
  avatarUrl: string | null;
}

export interface ConversationProjectSummary {
  id: string;
  title: string;
  slug: string;
}

export interface MessagePreviewSummary {
  id: string;
  content: string;
  senderUsername: string;
  createdAt: string;
  messageType: MessageType;
}

export interface ConversationSummary {
  id: string;
  type: ConversationType;
  name: string;
  directParticipant: ConversationParticipantSummary | null;
  project: ConversationProjectSummary | null;
  lastMessage: MessagePreviewSummary | null;
  unreadCount: number;
  updatedAt: string;
}

export interface ConversationDetail {
  id: string;
  type: ConversationType;
  name: string;
  project: ConversationProjectSummary | null;
  participants: ConversationParticipantSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface MessageSenderSummary {
  userId: string;
  username: string;
  fullName: string;
  avatarUrl: string | null;
}

export interface MessageItem {
  id: string;
  conversationId: string;
  sender: MessageSenderSummary;
  content: string;
  messageType: MessageType;
  createdAt: string;
  optimistic?: boolean;
  failed?: boolean;
}

export interface SendMessagePayload {
  content: string;
}

export interface CreateDirectConversationRequest {
  username: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface TypingEvent {
  conversationId: string;
  userId: string;
  username: string;
  typing: boolean;
  timestamp: string;
}

export interface ReadReceiptEvent {
  conversationId: string;
  userId: string;
  username: string;
  lastReadAt: string;
}
