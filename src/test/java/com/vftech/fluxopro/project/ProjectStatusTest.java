package com.vftech.fluxopro.project;

import com.vftech.fluxopro.project.entity.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStatusTest {

    private static final Set<List<ProjectStatus>> ALLOWED = Set.of(
            List.of(ProjectStatus.PLANNING, ProjectStatus.IN_PROGRESS),
            List.of(ProjectStatus.PLANNING, ProjectStatus.CANCELLED),
            List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.DELIVERED),
            List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED));

    @Test
    void onlyTheDocumentedTransitionsAreAllowed() {
        for (ProjectStatus from : ProjectStatus.values()) {
            for (ProjectStatus to : ProjectStatus.values()) {
                boolean expected = ALLOWED.contains(List.of(from, to));
                assertThat(from.canMoveTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(expected);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"DELIVERED", "CANCELLED"})
    void finalStatusesAreClosedAndCannotMove(ProjectStatus status) {
        assertThat(status.isOpen()).isFalse();
        for (ProjectStatus target : ProjectStatus.values()) {
            assertThat(status.canMoveTo(target)).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = ProjectStatus.class, names = {"PLANNING", "IN_PROGRESS"})
    void openStatusesAreOpen(ProjectStatus status) {
        assertThat(status.isOpen()).isTrue();
        assertThat(ProjectStatus.openStatuses()).contains(status);
    }
}
