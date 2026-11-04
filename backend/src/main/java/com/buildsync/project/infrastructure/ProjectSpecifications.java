package com.buildsync.project.infrastructure;

import com.buildsync.profile.domain.Skill;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectTechnology;
import com.buildsync.project.domain.ProjectVisibility;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic JPA specifications for project discoverability and multi-criteria searching.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    /**
     * Builds a composite Specification for public project discovery.
     * Enforces visibility = PUBLIC and applies dynamic keyword, category, status, and technology filters.
     */
    public static Specification<Project> publicProjectsWithFilters(
            String keyword,
            ProjectCategory category,
            ProjectStatus status,
            String technology) {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule 1: Always enforce visibility = PUBLIC for public browsing
            predicates.add(builder.equal(root.get("visibility"), ProjectVisibility.PUBLIC));

            // Rule 2: Keyword search across title, shortDescription, and detailedDescription
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = builder.like(builder.lower(root.get("title")), pattern);
                Predicate shortDescMatch = builder.like(builder.lower(root.get("shortDescription")), pattern);
                Predicate detailedDescMatch = builder.like(
                        builder.lower(builder.coalesce(root.get("detailedDescription"), "")), pattern);
                predicates.add(builder.or(titleMatch, shortDescMatch, detailedDescMatch));
            }

            // Rule 3: Category filter
            if (category != null) {
                predicates.add(builder.equal(root.get("category"), category));
            }

            // Rule 4: Status filter
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            // Rule 5: Technology filter via subquery to avoid Cartesian product explosion
            if (technology != null && !technology.trim().isEmpty()) {
                Subquery<UUID> techSubquery = query.subquery(UUID.class);
                Root<ProjectTechnology> ptRoot = techSubquery.from(ProjectTechnology.class);
                Join<ProjectTechnology, Skill> skillJoin = ptRoot.join("skill");

                techSubquery.select(ptRoot.get("project").get("id"))
                        .where(builder.equal(
                                builder.lower(skillJoin.get("name")),
                                technology.trim().toLowerCase()
                        ));

                predicates.add(root.get("id").in(techSubquery));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
