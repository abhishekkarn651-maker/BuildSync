package com.buildsync.project.domain;

/**
 * Standard classification categories for software projects on BuildSync.
 */
public enum ProjectCategory {
    FULL_STACK("Full-Stack"),
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    MOBILE("Mobile"),
    AI_ML("AI & Machine Learning"),
    DEVOPS("DevOps & Cloud"),
    BLOCKCHAIN("Web3 & Blockchain"),
    CYBERSECURITY("Cybersecurity"),
    OPEN_SOURCE("Open Source Tools"),
    OTHER("Other");

    private final String displayName;

    ProjectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

