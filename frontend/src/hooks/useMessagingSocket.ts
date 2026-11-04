'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import {
  createStompClient,
  subscribeToConversation,
  publishChatMessage,
  publishTypingEvent,
} from '@/lib/stomp-client';
import { messagingService } from '@/services/messaging.service';
import { tokenStore } from '@/services/token-store';
import { MessageItem, ReadReceiptEvent, TypingEvent } from '@/types/messaging.types';

interface UseMessagingSocketOptions {
  conversationId: string | null;
  onMessageReceived?: (msg: MessageItem) => void;
  onTypingEvent?: (evt: TypingEvent) => void;
  onReadReceiptEvent?: (evt: ReadReceiptEvent) => void;
}

export function useMessagingSocket({
  conversationId,
  onMessageReceived,
  onTypingEvent,
  onReadReceiptEvent,
}: UseMessagingSocketOptions) {
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const stompClientRef = useRef<Client | null>(null);

  // Keep latest callbacks in refs to prevent unnecessary re-subscribes
  const onMessageRef = useRef(onMessageReceived);
  const onTypingRef = useRef(onTypingEvent);
  const onReadReceiptRef = useRef(onReadReceiptEvent);

  useEffect(() => {
    onMessageRef.current = onMessageReceived;
    onTypingRef.current = onTypingEvent;
    onReadReceiptRef.current = onReadReceiptEvent;
  });

  // Manage STOMP Client Lifecycle
  useEffect(() => {
    let isMounted = true;
    const token = tokenStore.getAccessToken();

    if (!token) {
      return;
    }

    const client = createStompClient({
      onConnect: () => {
        if (!isMounted) return;
        setIsConnected(true);
        setIsConnecting(false);
      },
      onDisconnect: () => {
        if (!isMounted) return;
        setIsConnected(false);
        setIsConnecting(false);
      },
      onError: () => {
        if (!isMounted) return;
        setIsConnected(false);
        setIsConnecting(false);
      },
    });

    if (client) {
      stompClientRef.current = client;
      client.activate();
    }

    // Subscribe to token changes
    const unsubscribeToken = tokenStore.subscribe((newToken) => {
      if (!newToken && stompClientRef.current) {
        stompClientRef.current.deactivate();
        setIsConnected(false);
        setIsConnecting(false);
      }
    });

    return () => {
      isMounted = false;
      unsubscribeToken();
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
    };
  }, []);

  // Manage Active Conversation Subscription
  useEffect(() => {
    if (!conversationId || !isConnected || !stompClientRef.current) {
      return;
    }

    const unsubscribe = subscribeToConversation(stompClientRef.current, {
      conversationId,
      onMessage: (msg) => {
        onMessageRef.current?.(msg);
      },
      onTyping: (evt) => {
        onTypingRef.current?.(evt);
      },
      onReadReceipt: (evt) => {
        onReadReceiptRef.current?.(evt);
      },
    });

    return () => {
      unsubscribe();
    };
  }, [conversationId, isConnected]);

  // Send message method (STOMP first, REST fallback)
  const sendMessage = useCallback(
    async (content: string): Promise<MessageItem | null> => {
      if (!conversationId) return null;

      // Try STOMP first
      const sentViaStomp = publishChatMessage(
        stompClientRef.current,
        conversationId,
        content
      );

      if (sentViaStomp) {
        return null; // Sent asynchronously via STOMP broker
      }

      // Fallback to REST
      try {
        const response = await messagingService.sendMessage(conversationId, content);
        return response;
      } catch (err) {
        console.error('Failed to send message via REST fallback:', err);
        throw err;
      }
    },
    [conversationId]
  );

  // Send typing indicator
  const sendTyping = useCallback(
    (typing: boolean) => {
      if (!conversationId) return;
      publishTypingEvent(stompClientRef.current, conversationId, typing);
    },
    [conversationId]
  );

  return {
    isConnected,
    isConnecting,
    sendMessage,
    sendTyping,
  };
}
