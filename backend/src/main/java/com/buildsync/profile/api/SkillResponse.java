package com.buildsync.profile.api;

import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;

import java.util.UUID;

/**
 * Safe DTO for master skills catalogue entries.
 */
public class SkillResponse {

    private UUID id;
    private String name;
    private SkillCategory category;

    public SkillResponse() {
    }

    public SkillResponse(UUID id, String name, SkillCategory category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public static SkillResponse fromEntity(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}

