package com.vftech.fluxopro.project.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    DELIVERED,
    CANCELLED;

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED = new EnumMap<>(ProjectStatus.class);

    static {
        ALLOWED.put(PLANNING, EnumSet.of(IN_PROGRESS, CANCELLED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(DELIVERED, CANCELLED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(ProjectStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(ProjectStatus.class));
    }

    public boolean canMoveTo(ProjectStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public boolean isOpen() {
        return this == PLANNING || this == IN_PROGRESS;
    }

    public static Set<ProjectStatus> openStatuses() {
        return EnumSet.of(PLANNING, IN_PROGRESS);
    }

}
