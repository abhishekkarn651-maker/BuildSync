package com.buildsync.messaging.infrastructure;

import com.buildsync.messaging.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDirectConversationKey(String directConversationKey);

    Optional<Conversation> findByProjectId(UUID projectId);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipantUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c " +
            "LEFT JOIN FETCH c.participants p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH c.project " +
            "WHERE c.id = :id")
    Optional<Conversation> findByIdWithDetails(@Param("id") UUID id);
}

