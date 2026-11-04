package com.buildsync.messaging.infrastructure;

import com.buildsync.messaging.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender s " +
            "WHERE m.conversation.id = :conversationId " +
            "ORDER BY m.createdAt ASC")
    Page<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender s " +
            "WHERE m.conversation.id = :conversationId AND m.createdAt < :beforeTimestamp " +
            "ORDER BY m.createdAt DESC")
    List<Message> findMessagesBeforeTimestamp(
            @Param("conversationId") UUID conversationId,
            @Param("beforeTimestamp") Instant beforeTimestamp,
            Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.createdAt > :lastReadAt " +
            "AND m.sender.id <> :userId")
    long countUnreadMessages(
            @Param("conversationId") UUID conversationId,
            @Param("lastReadAt") Instant lastReadAt,
            @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM Message m " +
            "JOIN ConversationParticipant p ON m.conversation.id = p.conversation.id " +
            "WHERE p.user.id = :userId " +
            "AND m.sender.id <> :userId " +
            "AND m.createdAt > p.lastReadAt")
    long countTotalUnreadMessagesForUser(@Param("userId") UUID userId);
}

