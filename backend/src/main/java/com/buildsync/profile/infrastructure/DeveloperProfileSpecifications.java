package com.buildsync.profile.infrastructure;

import com.buildsync.auth.domain.User;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.ExperienceLevel;
import com.buildsync.profile.domain.Skill;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification builder for dynamic multi-criteria developer search and discoverability.
 */
public class DeveloperProfileSpecifications {

    private DeveloperProfileSpecifications() {
    }

    public static Specification<DeveloperProfile> withFilters(
            String keyword,
            String skill,
            ExperienceLevel experienceLevel,
            AvailabilityStatus availability,
            String location) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Join with User
            Join<DeveloperProfile, User> userJoin = root.join("user", JoinType.INNER);

            // 2. Keyword filter across name, username, headline, bio, location
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), pattern);
                Predicate usernameMatch = cb.like(cb.lower(userJoin.get("username")), pattern);
                Predicate headlineMatch = cb.like(cb.lower(root.get("headline")), pattern);
                Predicate bioMatch = cb.like(cb.lower(root.get("bio")), pattern);
                Predicate locationMatch = cb.like(cb.lower(root.get("location")), pattern);

                predicates.add(cb.or(nameMatch, usernameMatch, headlineMatch, bioMatch, locationMatch));
            }

            // 3. Skill filter using subquery to prevent Cartesian duplicate rows
            if (skill != null && !skill.trim().isEmpty()) {
                String cleanSkill = skill.trim().toLowerCase();
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<DeveloperProfileSkill> dpsRoot = subquery.from(DeveloperProfileSkill.class);
                Join<DeveloperProfileSkill, Skill> skillJoin = dpsRoot.join("skill", JoinType.INNER);

                subquery.select(dpsRoot.get("profile").get("id"))
                        .where(cb.equal(cb.lower(skillJoin.get("name")), cleanSkill));

                predicates.add(root.get("id").in(subquery));
            }

            // 4. Experience Level filter
            if (experienceLevel != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            }

            // 5. Availability Status filter
            if (availability != null) {
                predicates.add(cb.equal(root.get("availabilityStatus"), availability));
            }

            // 6. Location filter
            if (location != null && !location.trim().isEmpty()) {
                String locPattern = "%" + location.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("location")), locPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

