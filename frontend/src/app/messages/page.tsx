'use client';

import React, { useState, useEffect, useRef, useCallback, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import {
  Send,
  Search,
  CheckCheck,
  ArrowLeft,
  User,
  Plus,
  Users,
  FolderGit2,
  AlertCircle,
  Clock,
  Sparkles,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { useAuth } from '@/context/AuthContext';
import { messagingService } from '@/services/messaging.service';
import { useMessagingSocket } from '@/hooks/useMessagingSocket';
import {
  ConversationSummary,
  ConversationDetail,
  MessageItem,
  TypingEvent,
  ReadReceiptEvent,
} from '@/types/messaging.types';
import { getAvatarUrl } from '@/config/api.config';

function formatMessageTime(dateString: string): string {
  if (!dateString) return '';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return '';
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatConversationTime(dateString: string): string {
  if (!dateString) return '';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return '';
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();

  if (isToday) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  if (isYesterday) {
    return 'Yesterday';
  }
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function MessagesContent() {
  const searchParams = useSearchParams();
  const { user, status, isAuthenticated } = useAuth();

  // URL Query Parameters
  const paramConvId = searchParams.get('conversationId');
  const paramUser = searchParams.get('user');
  const paramProject = searchParams.get('project');

  // State
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [activeConvDetail, setActiveConvDetail] = useState<ConversationDetail | null>(null);
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [isLoadingConversations, setIsLoadingConversations] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [inputText, setInputText] = useState('');
  const [mobileChatView, setMobileChatView] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  // New Conversation Modal State
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newChatError, setNewChatError] = useState<string | null>(null);
  const [isStartingChat, setIsStartingChat] = useState(false);

  // Typing state: username -> expiration timestamp
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const typingTimerRef = useRef<NodeJS.Timeout | null>(null);
  const isTypingRef = useRef(false);

  // Message list bottom ref for auto-scrolling
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  const scrollToBottom = useCallback((smooth = false) => {
    messagesEndRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto' });
  }, []);

  // Real-Time STOMP Callbacks
  const handleIncomingMessage = useCallback(
    (msg: MessageItem) => {
      const msgSenderId = msg.sender.userId || msg.sender.id;
      // 1. If incoming message belongs to active conversation, append or reconcile
      if (msg.conversationId === activeConvId) {
        setMessages((prev) => {
          // Reconcile optimistic messages
          const existingIndex = prev.findIndex(
            (m) =>
              m.id === msg.id ||
              (m.optimistic &&
                (m.sender.userId || m.sender.id) === msgSenderId &&
                m.content === msg.content)
          );
          if (existingIndex !== -1) {
            const next = [...prev];
            next[existingIndex] = msg;
            return next;
          }
          return [...prev, msg];
        });

        // Mark as read immediately since user is in the room
        messagingService.markAsRead(activeConvId).catch(() => {});
        setTimeout(() => scrollToBottom(true), 50);
      }

      // 2. Update conversation preview and ordering in conversations list
      setConversations((prev) => {
        const foundIndex = prev.findIndex((c) => c.id === msg.conversationId);
        if (foundIndex === -1) {
          // New conversation created out-of-band: trigger refresh
          messagingService.getUserConversations(0, 50).then((res) => {
            setConversations(res.content);
          }).catch(() => {});
          return prev;
        }

        const existing = prev[foundIndex];
        const updated: ConversationSummary = {
          ...existing,
          lastMessage: {
            id: msg.id,
            content: msg.content,
            senderUsername: msg.sender.username,
            createdAt: msg.createdAt,
            messageType: msg.messageType,
          },
          updatedAt: msg.createdAt,
          unreadCount:
            msg.conversationId === activeConvId
              ? 0
              : existing.unreadCount + (msgSenderId === user?.id ? 0 : 1),
        };

        const remaining = prev.filter((c) => c.id !== msg.conversationId);
        return [updated, ...remaining];
      });
    },
    [activeConvId, scrollToBottom, user?.id]
  );

  const handleTypingEvent = useCallback(
    (evt: TypingEvent) => {
      if (evt.conversationId !== activeConvId || evt.userId === user?.id) {
        return;
      }

      setTypingUsers((prev) => {
        if (evt.typing) {
          if (!prev.includes(evt.username)) {
            return [...prev, evt.username];
          }
          return prev;
        } else {
          return prev.filter((u) => u !== evt.username);
        }
      });

      // Clear indicator automatically after 3.5 seconds if no follow-up received
      if (evt.typing) {
        setTimeout(() => {
          setTypingUsers((prev) => prev.filter((u) => u !== evt.username));
        }, 3500);
      }
    },
    [activeConvId, user?.id]
  );

  const handleReadReceiptEvent = useCallback(
    (evt: ReadReceiptEvent) => {
      if (evt.conversationId !== activeConvId) return;
    },
    [activeConvId]
  );

  // Hook into WebSocket / STOMP
  const { isConnected, isConnecting, sendMessage: sendViaSocket, sendTyping } =
    useMessagingSocket({
      conversationId: activeConvId,
      onMessageReceived: handleIncomingMessage,
      onTypingEvent: handleTypingEvent,
      onReadReceiptEvent: handleReadReceiptEvent,
    });

  // Load conversations on mount & handle URL query parameters
  useEffect(() => {
    let isMounted = true;

    if (status !== 'authenticated' || !isAuthenticated) {
      return;
    }

    async function initConversations() {
      setIsLoadingConversations(true);
      try {
        const response = await messagingService.getUserConversations(0, 50);
        if (!isMounted) return;

        let list = response.content;
        setConversations(list);

        // Check if navigation requested a specific direct user
        if (paramUser) {
          try {
            const directConv = await messagingService.getOrCreateDirectConversation(paramUser);
            if (!list.some((c) => c.id === directConv.id)) {
              list = [directConv, ...list];
              setConversations(list);
            }
            setActiveConvId(directConv.id);
            setMobileChatView(true);
            return;
          } catch (e: unknown) {
            console.error('Failed to open direct conversation with user', e);
          }
        }

        // Check if navigation requested a specific project room
        if (paramProject) {
          try {
            const projectConv = await messagingService.getOrCreateProjectConversation(paramProject);
            if (!list.some((c) => c.id === projectConv.id)) {
              list = [projectConv, ...list];
              setConversations(list);
            }
            setActiveConvId(projectConv.id);
            setMobileChatView(true);
            return;
          } catch (e: unknown) {
            console.error('Failed to open project conversation', e);
          }
        }

        // Check if navigation requested a specific conversationId
        if (paramConvId) {
          const match = list.find((c) => c.id === paramConvId);
          if (match) {
            setActiveConvId(match.id);
            setMobileChatView(true);
            return;
          } else {
            try {
              const detail = await messagingService.getConversationDetail(paramConvId);
              setActiveConvId(detail.id);
              setMobileChatView(true);
              return;
            } catch {
              // Fallback
            }
          }
        }

        // Default to first conversation if on desktop
        if (list.length > 0 && typeof window !== 'undefined' && window.innerWidth >= 768) {
          setActiveConvId(list[0].id);
        }
      } catch (err) {
        console.error('Failed to load conversations', err);
        setErrorBanner('Unable to load conversations. Please refresh or try again.');
      } finally {
        if (isMounted) {
          setIsLoadingConversations(false);
        }
      }
    }

    initConversations();

    return () => {
      isMounted = false;
    };
  }, [status, isAuthenticated, paramConvId, paramUser, paramProject]);

  // Load message history & conversation detail when active conversation changes
  useEffect(() => {
    let isMounted = true;
    if (!activeConvId || status !== 'authenticated' || !isAuthenticated) {
      return;
    }

    async function loadActiveConversation() {
      setIsLoadingMessages(true);
      setTypingUsers([]);
      try {
        const [messagesRes, detailRes] = await Promise.all([
          messagingService.getMessageHistory(activeConvId!, 0, 100),
          messagingService.getConversationDetail(activeConvId!),
        ]);

        if (!isMounted) return;

        setMessages(messagesRes.content);
        setActiveConvDetail(detailRes);

        // Mark as read in backend
        await messagingService.markAsRead(activeConvId!);

        // Clear unread count locally
        setConversations((prev) =>
          prev.map((c) => (c.id === activeConvId ? { ...c, unreadCount: 0 } : c))
        );

        setTimeout(() => scrollToBottom(false), 50);
      } catch (err) {
        console.error('Failed to load active conversation messages', err);
      } finally {
        if (isMounted) {
          setIsLoadingMessages(false);
        }
      }
    }

    loadActiveConversation();

    return () => {
      isMounted = false;
    };
  }, [activeConvId, status, isAuthenticated, scrollToBottom]);

  // Handle typing input
  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>) => {
    setInputText(e.target.value);

    if (!isTypingRef.current) {
      isTypingRef.current = true;
      sendTyping(true);
    }

    if (typingTimerRef.current) {
      clearTimeout(typingTimerRef.current);
    }

    typingTimerRef.current = setTimeout(() => {
      isTypingRef.current = false;
      sendTyping(false);
    }, 2000);
  };

  // Handle message sending
  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const content = inputText.trim();
    if (!content || !activeConvId || isSending || !user) return;

    if (content.length > 4000) {
      alert('Message exceeds maximum limit of 4000 characters.');
      return;
    }

    // Reset typing status
    if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
    isTypingRef.current = false;
    sendTyping(false);

    const tempId = `temp-${Date.now()}`;
    const optimisticMsg: MessageItem = {
      id: tempId,
      conversationId: activeConvId,
      sender: {
        id: user.id,
        userId: user.id,
        username: user.username,
        fullName: user.fullName || user.username || 'You',
        avatarUrl: user.avatarUrl || null,
      },
      content,
      messageType: 'TEXT',
      createdAt: new Date().toISOString(),
      optimistic: true,
    };

    // Optimistic UI update
    setMessages((prev) => [...prev, optimisticMsg]);
    setInputText('');
    setIsSending(true);
    setTimeout(() => scrollToBottom(true), 50);

    // Update conversation list preview optimistically
    setConversations((prev) => {
      const idx = prev.findIndex((c) => c.id === activeConvId);
      if (idx === -1) return prev;
      const current = prev[idx];
      const updated: ConversationSummary = {
        ...current,
        lastMessage: {
          id: tempId,
          content,
          senderUsername: user.username,
          createdAt: optimisticMsg.createdAt,
          messageType: 'TEXT',
        },
        updatedAt: optimisticMsg.createdAt,
      };
      const rest = prev.filter((c) => c.id !== activeConvId);
      return [updated, ...rest];
    });

    try {
      const restResponse = await sendViaSocket(content);
      if (restResponse) {
        // Reconcile optimistic message if sent via REST fallback
        setMessages((prev) =>
          prev.map((m) => (m.id === tempId ? restResponse : m))
        );
      }
    } catch (err) {
      console.error('Failed to send message:', err);
      setMessages((prev) =>
        prev.map((m) => (m.id === tempId ? { ...m, failed: true, optimistic: false } : m))
      );
    } finally {
      setIsSending(false);
    }
  };

  // Keyboard shortcut: Enter to send, Shift+Enter for newline
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // Start new direct conversation via modal
  const handleStartNewChat = async (e: React.FormEvent) => {
    e.preventDefault();
    const target = newUsername.trim();
    if (!target) return;

    if (user?.username && target.toLowerCase() === user.username.toLowerCase()) {
      setNewChatError('You cannot start a direct conversation with yourself.');
      return;
    }

    setIsStartingChat(true);
    setNewChatError(null);

    try {
      const conv = await messagingService.getOrCreateDirectConversation(target);
      setIsNewModalOpen(false);
      setNewUsername('');

      setConversations((prev) => {
        if (prev.some((c) => c.id === conv.id)) return prev;
        return [conv, ...prev];
      });

      setActiveConvId(conv.id);
      setMobileChatView(true);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Developer not found or failed to create conversation.';
      setNewChatError(message);
    } finally {
      setIsStartingChat(false);
    }
  };

  // Filter conversations
  const filteredConversations = conversations.filter((c) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const participant = c.directParticipant || c.otherParticipant;
    const directName = participant?.fullName?.toLowerCase() || '';
    const directUser = participant?.username?.toLowerCase() || '';
    const projectName = c.project?.title?.toLowerCase() || '';
    const lastMsg = c.lastMessage?.content?.toLowerCase() || '';
    return (
      directName.includes(q) ||
      directUser.includes(q) ||
      projectName.includes(q) ||
      lastMsg.includes(q)
    );
  });

  const activeConversation = conversations.find((c) => c.id === activeConvId);

  return (
    <AppLayout requireAuth>
      {errorBanner && (
        <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-400" />
            <span>{errorBanner}</span>
          </div>
          <button
            type="button"
            onClick={() => setErrorBanner(null)}
            className="text-xs text-muted hover:text-foreground cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      <div className="h-[calc(100vh-8.5rem)] min-h-[550px] flex rounded-2xl bg-card border border-border overflow-hidden shadow-2xl relative">
        {/* Left Sidebar: Conversations Directory */}
        <div
          className={`w-full md:w-80 lg:w-96 border-r border-border flex flex-col bg-card shrink-0 ${
            mobileChatView ? 'hidden md:flex' : 'flex'
          }`}
        >
          {/* Header & New Chat Button */}
          <div className="p-4 border-b border-border space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold text-foreground tracking-tight">
                  Messages
                </h1>
                {isConnected ? (
                  <span
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
                    title="Real-Time WebSocket Active"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    Live
                  </span>
                ) : isConnecting ? (
                  <span
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/10 text-amber-500 border border-amber-500/20"
                    title="Connecting to message broker..."
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                    Connecting
                  </span>
                ) : (
                  <span
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-zinc-500/10 text-zinc-400 border border-zinc-500/20"
                    title="Operating via Secure REST Fallback"
                  >
                    REST Fallback
                  </span>
                )}
              </div>

              <Button
                variant="primary"
                size="sm"
                className="h-8 px-2.5 text-xs font-semibold"
                leftIcon={<Plus className="w-3.5 h-3.5" />}
                onClick={() => {
                  setNewChatError(null);
                  setNewUsername('');
                  setIsNewModalOpen(true);
                }}
              >
                New Chat
              </Button>
            </div>

            {/* Search Input */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search conversations, projects..."
                className="w-full h-9 pl-9 pr-3 rounded-lg bg-surface border border-border text-xs text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444] transition-colors"
              />
            </div>
          </div>

          {/* Conversations List */}
          <div className="flex-1 overflow-y-auto divide-y divide-border/60">
            {isLoadingConversations ? (
              <div className="p-6 space-y-4">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="flex items-center gap-3 animate-pulse">
                    <div className="w-10 h-10 rounded-full bg-surface" />
                    <div className="flex-1 space-y-2">
                      <div className="h-3 bg-surface rounded w-3/4" />
                      <div className="h-2 bg-surface rounded w-1/2" />
                    </div>
                  </div>
                ))}
              </div>
            ) : filteredConversations.length === 0 ? (
              <div className="p-8 text-center space-y-3">
                <div className="w-12 h-12 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto text-[#EF4444]">
                  <Sparkles className="w-5 h-5" />
                </div>
                <h3 className="text-sm font-bold text-foreground">No conversations yet</h3>
                <p className="text-xs text-muted max-w-xs mx-auto">
                  Start a direct chat with another developer or collaborate on a project.
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  className="text-xs"
                  onClick={() => setIsNewModalOpen(true)}
                >
                  Start New Chat
                </Button>
              </div>
            ) : (
              filteredConversations.map((conv) => {
                const isActive = conv.id === activeConvId;
                const isProject = conv.type === 'PROJECT' || conv.conversationType === 'PROJECT';
                const participant = conv.directParticipant || conv.otherParticipant;
                const displayName = isProject
                  ? conv.project?.title || conv.name || 'Project Room'
                  : participant?.fullName || participant?.username || conv.name || 'Developer';

                return (
                  <button
                    key={conv.id}
                    type="button"
                    onClick={() => {
                      setActiveConvId(conv.id);
                      setMobileChatView(true);
                    }}
                    className={`w-full p-4 flex items-start gap-3 text-left transition-all cursor-pointer ${
                      isActive
                        ? 'bg-red-500/10 border-l-4 border-l-[#EF4444]'
                        : 'hover:bg-surface'
                    }`}
                  >
                    {isProject ? (
                      <div className="w-10 h-10 rounded-xl bg-red-500/15 border border-red-500/30 flex items-center justify-center text-[#EF4444] shrink-0 font-bold">
                        <FolderGit2 className="w-5 h-5" />
                      </div>
                    ) : (
                      <Avatar
                        src={getAvatarUrl(participant?.avatarUrl)}
                        name={displayName}
                        size="md"
                      />
                    )}

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-0.5">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <h4
                            className={`text-xs font-bold truncate ${
                              isActive ? 'text-[#EF4444]' : 'text-foreground'
                            }`}
                          >
                            {displayName}
                          </h4>
                          {isProject && (
                            <span className="text-[9px] px-1.5 py-0.2 rounded bg-amber-500/15 text-amber-500 border border-amber-500/30 font-semibold shrink-0">
                              Project
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-muted whitespace-nowrap ml-1">
                          {formatConversationTime(conv.updatedAt)}
                        </span>
                      </div>

                      <div className="flex items-center justify-between gap-1">
                        <p className="text-xs text-muted truncate">
                          {conv.lastMessage ? (
                            <>
                              <span className="font-medium text-foreground/80">
                                {conv.lastMessage.senderUsername === user?.username
                                  ? 'You: '
                                  : `${conv.lastMessage.senderUsername}: `}
                              </span>
                              {conv.lastMessage.content}
                            </>
                          ) : (
                            <span className="italic text-muted/60">No messages yet</span>
                          )}
                        </p>

                        {conv.unreadCount > 0 && !isActive && (
                          <span className="flex items-center justify-center min-w-[18px] h-4.5 px-1.5 text-[10px] font-bold rounded-full bg-[#EF4444] text-white shrink-0 shadow-sm animate-pulse">
                            {conv.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Right Main Chat Area */}
        <div
          className={`flex-1 flex flex-col bg-background min-w-0 ${
            !mobileChatView ? 'hidden md:flex' : 'flex'
          }`}
        >
          {activeConvId && activeConversation ? (
            <>
              {/* Chat Header */}
              <div className="h-16 px-4 sm:px-6 border-b border-border bg-card flex items-center justify-between gap-3 shrink-0">
                <div className="flex items-center gap-3 min-w-0">
                  <button
                    type="button"
                    onClick={() => setMobileChatView(false)}
                    className="md:hidden p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-surface cursor-pointer"
                    aria-label="Back to conversations list"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>

                  {(() => {
                    const isActiveProject = activeConversation.type === 'PROJECT' || activeConversation.conversationType === 'PROJECT';
                    const activeParticipant = activeConversation.directParticipant || activeConversation.otherParticipant;
                    const activeDisplayName = isActiveProject
                      ? activeConversation.project?.title || activeConversation.name || 'Project Room'
                      : activeParticipant?.fullName || activeParticipant?.username || activeConversation.name || 'Developer';

                    return (
                      <>
                        {isActiveProject ? (
                          <div className="w-9 h-9 rounded-xl bg-red-500/15 border border-red-500/30 flex items-center justify-center text-[#EF4444] shrink-0">
                            <FolderGit2 className="w-5 h-5" />
                          </div>
                        ) : (
                          <Avatar
                            src={getAvatarUrl(activeParticipant?.avatarUrl)}
                            name={activeDisplayName}
                            size="sm"
                          />
                        )}

                        <div className="min-w-0">
                          <h3 className="text-sm font-bold text-foreground flex items-center gap-2 truncate">
                            <span>{activeDisplayName}</span>
                          </h3>
                          <p className="text-[11px] text-muted truncate">
                            {isActiveProject ? (
                              <span className="flex items-center gap-1.5 text-muted">
                                <Users className="w-3 h-3 text-[#EF4444]" />
                                <span>Team Chat</span>
                                {activeConvDetail?.participants && (
                                  <span>• {activeConvDetail.participants.length} members</span>
                                )}
                              </span>
                            ) : (
                              <span>@{activeParticipant?.username || 'user'}</span>
                            )}
                          </p>
                        </div>
                      </>
                    );
                  })()}
                </div>

                {/* Right controls */}
                <div className="flex items-center gap-2 shrink-0">
                  {(() => {
                    const isActiveProject = activeConversation.type === 'PROJECT' || activeConversation.conversationType === 'PROJECT';
                    const activeParticipant = activeConversation.directParticipant || activeConversation.otherParticipant;

                    return (
                      <>
                        {!isActiveProject && activeParticipant?.username && (
                          <Link
                            href={`/developers/${activeParticipant.username}`}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-muted hover:text-foreground bg-surface hover:bg-surface-hover border border-border transition-colors"
                          >
                            <User className="w-3.5 h-3.5" />
                            <span className="hidden sm:inline">Profile</span>
                          </Link>
                        )}

                        {isActiveProject && activeConversation.project?.id && (
                          <Link
                            href={`/projects/${activeConversation.project.id}`}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-muted hover:text-foreground bg-surface hover:bg-surface-hover border border-border transition-colors"
                          >
                            <FolderGit2 className="w-3.5 h-3.5" />
                            <span className="hidden sm:inline">Project Details</span>
                          </Link>
                        )}
                      </>
                    );
                  })()}
                </div>
              </div>

              {/* Messages History */}
              <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-4">
                {isLoadingMessages ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="flex flex-col items-center gap-3">
                      <div className="w-7 h-7 border-2 border-[#EF4444] border-t-transparent rounded-full animate-spin" />
                      <span className="text-xs text-muted">Loading messages...</span>
                    </div>
                  </div>
                ) : messages.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-3">
                    <div className="w-14 h-14 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center text-[#EF4444]">
                      <Send className="w-6 h-6 ml-0.5" />
                    </div>
                    <h4 className="text-base font-bold text-foreground">
                      No messages here yet
                    </h4>
                    <p className="text-xs text-muted max-w-sm">
                      Send a message below to break the ice and start synchronizing on projects.
                    </p>
                  </div>
                ) : (
                  <>
                    <div className="text-center my-3">
                      <span className="text-[10px] font-semibold text-muted bg-surface px-3 py-1 rounded-full border border-border inline-flex items-center gap-1.5">
                        <Clock className="w-3 h-3 text-[#EF4444]" />
                        End of message history • Channel synchronized
                      </span>
                    </div>

                    {messages.map((msg) => {
                      const msgSenderId = msg.sender.userId || msg.sender.id;
                      const isMe = msgSenderId === user?.id;
                      const isSystem = msg.messageType === 'SYSTEM';

                      if (isSystem) {
                        return (
                          <div key={msg.id} className="text-center my-2">
                            <span className="text-[11px] italic text-muted bg-surface/80 px-3 py-1 rounded-full border border-border/80">
                              {msg.content}
                            </span>
                          </div>
                        );
                      }

                      return (
                        <div
                          key={msg.id}
                          className={`flex items-end gap-2.5 ${
                            isMe ? 'justify-end' : 'justify-start'
                          }`}
                        >
                          {!isMe && (
                            <Avatar
                              src={getAvatarUrl(msg.sender.avatarUrl)}
                              name={msg.sender.fullName || msg.sender.username || 'User'}
                              size="xs"
                            />
                          )}

                          <div
                            className={`max-w-[78%] sm:max-w-md rounded-2xl px-4 py-2.5 text-xs sm:text-sm space-y-1 shadow-sm leading-relaxed ${
                              isMe
                                ? 'bg-[#EF4444] text-white rounded-br-none'
                                : 'bg-card text-foreground border border-border rounded-bl-none'
                            } ${msg.optimistic ? 'opacity-70' : ''} ${
                              msg.failed ? 'border-red-500 bg-red-500/20' : ''
                            }`}
                          >
                            {!isMe && (activeConversation.type === 'PROJECT' || activeConversation.conversationType === 'PROJECT') && (
                              <p className="text-[11px] font-bold text-[#EF4444] mb-0.5">
                                @{msg.sender.username}
                              </p>
                            )}

                            <p className="whitespace-pre-wrap break-words">{msg.content}</p>

                            <div
                              className={`text-[10px] flex items-center justify-end gap-1 ${
                                isMe ? 'text-white/80' : 'text-muted'
                              }`}
                            >
                              <span>{formatMessageTime(msg.createdAt)}</span>
                              {isMe && (
                                <>
                                  {msg.optimistic ? (
                                    <Clock className="w-3 h-3 animate-spin" />
                                  ) : msg.failed ? (
                                    <span title="Failed to send">
                                      <AlertCircle className="w-3 h-3 text-red-300" />
                                    </span>
                                  ) : (
                                    <CheckCheck className="w-3 h-3" />
                                  )}
                                </>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}

                    <div ref={messagesEndRef} />
                  </>
                )}
              </div>

              {/* Typing Indicator Bar */}
              {typingUsers.length > 0 && (
                <div className="px-6 py-1.5 bg-card border-t border-border/50 text-[11px] text-muted flex items-center gap-2">
                  <span className="flex gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#EF4444] animate-bounce" />
                    <span className="w-1.5 h-1.5 rounded-full bg-[#EF4444] animate-bounce [animation-delay:0.2s]" />
                    <span className="w-1.5 h-1.5 rounded-full bg-[#EF4444] animate-bounce [animation-delay:0.4s]" />
                  </span>
                  <span>
                    {typingUsers.join(', ')} {typingUsers.length > 1 ? 'are' : 'is'} typing...
                  </span>
                </div>
              )}

              {/* Message Input Composer */}
              <form
                onSubmit={handleSendMessage}
                className="p-3 sm:p-4 border-t border-border bg-card flex items-end gap-2"
              >
                <div className="flex-1 relative">
                  <textarea
                    rows={1}
                    value={inputText}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    placeholder={`Message ${
                      activeConversation.type === 'PROJECT' || activeConversation.conversationType === 'PROJECT'
                        ? activeConversation.project?.title || activeConversation.name || 'Team'
                        : `@${(activeConversation.directParticipant || activeConversation.otherParticipant)?.username || 'Developer'}`
                    }... (Enter to send, Shift+Enter for new line)`}
                    maxLength={4000}
                    className="w-full resize-none max-h-32 min-h-[42px] px-4 py-2.5 rounded-xl bg-surface border border-border text-xs sm:text-sm text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444] transition-colors"
                  />
                  {inputText.length > 3500 && (
                    <span className="absolute right-3 bottom-2 text-[10px] text-muted">
                      {4000 - inputText.length} left
                    </span>
                  )}
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  className="h-[42px] px-4 font-semibold shrink-0 cursor-pointer"
                  rightIcon={<Send className="w-3.5 h-3.5" />}
                  disabled={!inputText.trim() || isSending}
                >
                  Send
                </Button>
              </form>
            </>
          ) : (
            /* Empty State when no conversation selected */
            <div className="flex-1 flex flex-col items-center justify-center text-center p-8 space-y-4">
              <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center text-[#EF4444]">
                <Send className="w-8 h-8 ml-1" />
              </div>
              <h2 className="text-xl font-bold text-foreground">Select a Conversation</h2>
              <p className="text-xs sm:text-sm text-muted max-w-md">
                Choose a conversation from the sidebar to view message history, or start a new direct conversation with any developer.
              </p>
              <Button
                variant="primary"
                size="md"
                leftIcon={<Plus className="w-4 h-4" />}
                onClick={() => setIsNewModalOpen(true)}
              >
                Start New Chat
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Start Direct Conversation Modal */}
      <Modal
        isOpen={isNewModalOpen}
        onClose={() => {
          setIsNewModalOpen(false);
          setNewChatError(null);
        }}
        title="Start Direct Conversation"
        description="Connect and chat directly with another developer on BuildSync."
      >
        <form onSubmit={handleStartNewChat} className="space-y-4 pt-2">
          {newChatError && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
              <span>{newChatError}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-foreground">
              Developer Username <span className="text-[#EF4444]">*</span>
            </label>
            <Input
              value={newUsername}
              onChange={(e) => setNewUsername(e.target.value)}
              placeholder="e.g. sarah_chen"
              autoFocus
              required
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsNewModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={!newUsername.trim() || isStartingChat}
              leftIcon={<Send className="w-3.5 h-3.5" />}
            >
              {isStartingChat ? 'Connecting...' : 'Start Chat'}
            </Button>
          </div>
        </form>
      </Modal>
    </AppLayout>
  );
}

export default function MessagesPage() {
  return (
    <ProtectedRoute>
      <Suspense
        fallback={
          <AppLayout>
            <div className="h-[calc(100vh-8.5rem)] flex items-center justify-center">
              <div className="w-8 h-8 border-2 border-[#EF4444] border-t-transparent rounded-full animate-spin" />
            </div>
          </AppLayout>
        }
      >
        <MessagesContent />
      </Suspense>
    </ProtectedRoute>
  );
}
