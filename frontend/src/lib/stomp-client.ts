'use client';

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { getWebSocketUrl } from '@/config/api.config';
import { tokenStore } from '@/services/token-store';
import { MessageItem, ReadReceiptEvent, TypingEvent } from '@/types/messaging.types';

export interface StompClientHandlers {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (err: unknown) => void;
}

/**
 * Creates and configures a STOMP client instance.
 * dynamically injects the latest in-memory JWT bearer token into CONNECT headers.
 */
export function createStompClient(handlers: StompClientHandlers = {}): Client | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const client = new Client({
    brokerURL: getWebSocketUrl(),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (msg: string) => {
      if (process.env.NODE_ENV === 'development') {
        // Suppress ping/pong heartbeat noise in console
        if (!msg.includes('>>> PING') && !msg.includes('<<< PONG')) {
          console.debug('[STOMP]', msg);
        }
      }
    },
  });

  client.beforeConnect = () => {
    const token = tokenStore.getAccessToken();
    client.connectHeaders = {
      Authorization: token ? `Bearer ${token}` : '',
    };
  };

  client.onConnect = () => {
    handlers.onConnect?.();
  };

  client.onDisconnect = () => {
    handlers.onDisconnect?.();
  };

  client.onStompError = (frame) => {
    console.error('[STOMP] Broker reported error:', frame.headers['message'], frame.body);
    handlers.onError?.(frame);
  };

  client.onWebSocketError = (event) => {
    console.warn('[STOMP] WebSocket transport error:', event);
    handlers.onError?.(event);
  };

  return client;
}

export interface ConversationSocketSubscription {
  conversationId: string;
  onMessage: (msg: MessageItem) => void;
  onTyping?: (evt: TypingEvent) => void;
  onReadReceipt?: (evt: ReadReceiptEvent) => void;
}

/**
 * Subscribes a connected client to all topics for a given conversation:
 * - /topic/conversations/{conversationId} (chat messages)
 * - /topic/conversations/{conversationId}/typing (ephemeral typing indicators)
 * - /topic/conversations/{conversationId}/read (read receipts)
 */
export function subscribeToConversation(
  client: Client,
  options: ConversationSocketSubscription
): () => void {
  const { conversationId, onMessage, onTyping, onReadReceipt } = options;
  const subscriptions: StompSubscription[] = [];

  if (!client.connected) {
    return () => {};
  }

  try {
    // 1. Chat messages topic
    const msgSub = client.subscribe(
      `/topic/conversations/${conversationId}`,
      (message: IMessage) => {
        try {
          const raw = JSON.parse(message.body);
          const uid = raw?.sender?.userId || raw?.sender?.id || '';
          const sender = raw?.sender
            ? {
                ...raw.sender,
                userId: uid,
                id: raw.sender.id || uid,
                fullName: raw.sender.fullName || raw.sender.username || 'User',
              }
            : {
                userId: '',
                username: 'User',
                fullName: 'User',
                avatarUrl: null,
              };
          const payload: MessageItem = {
            ...raw,
            sender,
          };
          onMessage(payload);
        } catch (e) {
          console.error('Failed to parse incoming chat message payload', e);
        }
      }
    );
    subscriptions.push(msgSub);

    // 2. Typing indicators topic
    if (onTyping) {
      const typingSub = client.subscribe(
        `/topic/conversations/${conversationId}/typing`,
        (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as TypingEvent;
            onTyping(payload);
          } catch (e) {
            console.error('Failed to parse incoming typing payload', e);
          }
        }
      );
      subscriptions.push(typingSub);
    }

    // 3. Read receipts topic
    if (onReadReceipt) {
      const readSub = client.subscribe(
        `/topic/conversations/${conversationId}/read`,
        (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as ReadReceiptEvent;
            onReadReceipt(payload);
          } catch (e) {
            console.error('Failed to parse incoming read receipt payload', e);
          }
        }
      );
      subscriptions.push(readSub);
    }
  } catch (err) {
    console.error(`Failed to subscribe to conversation ${conversationId}`, err);
  }

  return () => {
    subscriptions.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // Ignored if already unsubscribed
      }
    });
  };
}

/**
 * Sends a chat message over the STOMP protocol.
 */
export function publishChatMessage(
  client: Client | null,
  conversationId: string,
  content: string
): boolean {
  if (!client || !client.connected) {
    return false;
  }
  try {
    client.publish({
      destination: `/app/conversations/${conversationId}/send`,
      body: JSON.stringify({ content }),
    });
    return true;
  } catch (err) {
    console.warn('Failed to publish message via STOMP, will fallback to REST', err);
    return false;
  }
}

/**
 * Sends an ephemeral typing event over STOMP.
 */
export function publishTypingEvent(
  client: Client | null,
  conversationId: string,
  typing: boolean
): void {
  if (!client || !client.connected) {
    return;
  }
  try {
    client.publish({
      destination: `/app/conversations/${conversationId}/typing`,
      body: JSON.stringify({ typing }),
    });
  } catch {
    // Ephemeral, silent fail
  }
}

