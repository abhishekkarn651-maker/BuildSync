package com.buildsync.messaging.infrastructure;

import com.buildsync.messaging.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadAt = :readAt WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    int updateLastReadAt(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId, @Param("readAt") Instant readAt);
}

