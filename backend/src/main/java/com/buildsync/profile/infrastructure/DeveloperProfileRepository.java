package com.buildsync.profile.infrastructure;

import com.buildsync.profile.domain.DeveloperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for DeveloperProfile entities.
 */
@Repository
public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, UUID>, JpaSpecificationExecutor<DeveloperProfile> {

    @Query("SELECT p FROM DeveloperProfile p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<DeveloperProfile> findByUserIdWithUser(@Param("userId") UUID userId);

    @Query("SELECT p FROM DeveloperProfile p JOIN FETCH p.user WHERE LOWER(p.user.username) = LOWER(:username)")
    Optional<DeveloperProfile> findByUsernameWithUser(@Param("username") String username);

    @Query("SELECT p FROM DeveloperProfile p JOIN FETCH p.user WHERE p.user.id IN :userIds")
    java.util.List<DeveloperProfile> findByUserIdInWithUser(@Param("userIds") java.util.Collection<UUID> userIds);

    Optional<DeveloperProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}

