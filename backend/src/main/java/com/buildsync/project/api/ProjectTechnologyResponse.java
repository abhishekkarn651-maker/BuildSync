package com.buildsync.project.api;

import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;

import java.util.UUID;

/**
 * Representation of a technical skill associated with a project.
 */
public class ProjectTechnologyResponse {

    private UUID id;
    private String name;
    private SkillCategory category;

    public ProjectTechnologyResponse() {
    }

    public ProjectTechnologyResponse(UUID id, String name, SkillCategory category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public static ProjectTechnologyResponse fromSkill(Skill skill) {
        return new ProjectTechnologyResponse(
                skill.getId(),
                skill.getName(),
                skill.getCategory()
        );
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

