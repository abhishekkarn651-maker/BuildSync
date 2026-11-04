-- ============================================================================
-- Migration: V9__create_messaging_and_conversation_tables.sql
-- Description: Creates conversations, conversation_participants, and messages tables
--              with constraints, partial unique indexes, and performance indexes.
-- ============================================================================

-- 1. Create conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_type VARCHAR(30) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    direct_conversation_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_conversation_type CHECK (conversation_type IN ('DIRECT', 'PROJECT')),
    CONSTRAINT chk_conversation_type_fields CHECK (
        (conversation_type = 'DIRECT' AND direct_conversation_key IS NOT NULL AND project_id IS NULL) OR
        (conversation_type = 'PROJECT' AND project_id IS NOT NULL AND direct_conversation_key IS NULL)
    )
);

-- Partial unique indexes ensuring one direct conversation per developer pair and one conversation per project
CREATE UNIQUE INDEX IF NOT EXISTS uq_direct_conversation_key
    ON conversations (direct_conversation_key)
    WHERE conversation_type = 'DIRECT';

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_conversation
    ON conversations (project_id)
    WHERE conversation_type = 'PROJECT';

CREATE INDEX IF NOT EXISTS idx_conversations_project_id ON conversations(project_id);
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON conversations(updated_at DESC);

-- 2. Create conversation_participants table
CREATE TABLE IF NOT EXISTS conversation_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_conversation_participant UNIQUE (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conv_participants_user_id ON conversation_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_conv_participants_conv_id ON conversation_participants(conversation_id);

-- 3. Create messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(4000) NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_message_type CHECK (message_type IN ('TEXT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON messages(conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);

