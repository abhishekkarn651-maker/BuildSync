package com.buildsync.profile.api;

import com.buildsync.profile.domain.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to update proficiency level for an already assigned skill.
 */
public class UpdateSkillProficiencyRequest {

    @NotNull(message = "Proficiency level is required")
    private ProficiencyLevel proficiencyLevel;

    public UpdateSkillProficiencyRequest() {
    }

    public UpdateSkillProficiencyRequest(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}

