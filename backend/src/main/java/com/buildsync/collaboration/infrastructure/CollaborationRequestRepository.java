package com.buildsync.collaboration.infrastructure;

import com.buildsync.collaboration.domain.CollaborationRequest;
import com.buildsync.collaboration.domain.CollaborationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CollaborationRequest entities.
 */
@Repository
public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, UUID> {

    boolean existsByProjectIdAndRequesterIdAndStatus(UUID projectId, UUID requesterId, CollaborationRequestStatus status);

    boolean existsByProjectIdAndRecipientIdAndStatus(UUID projectId, UUID recipientId, CollaborationRequestStatus status);

    @Query(
            value = "SELECT cr FROM CollaborationRequest cr " +
                    "JOIN FETCH cr.project p " +
                    "JOIN FETCH cr.requester req " +
                    "JOIN FETCH cr.recipient rec " +
                    "WHERE cr.recipient.id = :recipientId " +
                    "ORDER BY cr.createdAt DESC",
            countQuery = "SELECT COUNT(cr) FROM CollaborationRequest cr WHERE cr.recipient.id = :recipientId"
    )
    Page<CollaborationRequest> findByRecipientIdWithDetails(@Param("recipientId") UUID recipientId, Pageable pageable);

    @Query(
            value = "SELECT cr FROM CollaborationRequest cr " +
                    "JOIN FETCH cr.project p " +
                    "JOIN FETCH cr.requester req " +
                    "JOIN FETCH cr.recipient rec " +
                    "WHERE cr.requester.id = :requesterId " +
                    "ORDER BY cr.createdAt DESC",
            countQuery = "SELECT COUNT(cr) FROM CollaborationRequest cr WHERE cr.requester.id = :requesterId"
    )
    Page<CollaborationRequest> findByRequesterIdWithDetails(@Param("requesterId") UUID requesterId, Pageable pageable);

    @Query("SELECT cr FROM CollaborationRequest cr " +
            "JOIN FETCH cr.requester req " +
            "WHERE cr.project.id = :projectId AND cr.status = 'PENDING' " +
            "ORDER BY cr.createdAt DESC")
    List<CollaborationRequest> findPendingByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT cr FROM CollaborationRequest cr " +
            "JOIN FETCH cr.project p " +
            "JOIN FETCH p.owner o " +
            "JOIN FETCH cr.requester req " +
            "JOIN FETCH cr.recipient rec " +
            "WHERE cr.id = :id")
    Optional<CollaborationRequest> findByIdWithDetails(@Param("id") UUID id);

    long countByRecipientIdAndStatus(UUID recipientId, CollaborationRequestStatus status);
}

