package com.buildsync.profile.api;

import com.buildsync.profile.domain.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload to assign a skill to the authenticated developer's profile.
 */
public class AddSkillRequest {

    @NotNull(message = "Skill ID is required")
    private UUID skillId;

    @NotNull(message = "Proficiency level is required")
    private ProficiencyLevel proficiencyLevel;

    public AddSkillRequest() {
    }

    public AddSkillRequest(UUID skillId, ProficiencyLevel proficiencyLevel) {
        this.skillId = skillId;
        this.proficiencyLevel = proficiencyLevel;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}

