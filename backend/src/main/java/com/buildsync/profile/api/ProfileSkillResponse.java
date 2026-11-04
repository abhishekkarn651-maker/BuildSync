package com.buildsync.profile.api;

import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.ProficiencyLevel;
import com.buildsync.profile.domain.SkillCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for developer profile assigned skills with proficiency levels.
 */
public class ProfileSkillResponse {

    private UUID id;
    private UUID skillId;
    private String name;
    private SkillCategory category;
    private ProficiencyLevel proficiencyLevel;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfileSkillResponse() {
    }

    public ProfileSkillResponse(UUID id, UUID skillId, String name, SkillCategory category,
                                ProficiencyLevel proficiencyLevel, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.skillId = skillId;
        this.name = name;
        this.category = category;
        this.proficiencyLevel = proficiencyLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProfileSkillResponse fromEntity(DeveloperProfileSkill profileSkill) {
        return new ProfileSkillResponse(
                profileSkill.getId(),
                profileSkill.getSkill().getId(),
                profileSkill.getSkill().getName(),
                profileSkill.getSkill().getCategory(),
                profileSkill.getProficiencyLevel(),
                profileSkill.getCreatedAt(),
                profileSkill.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

