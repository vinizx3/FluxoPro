package com.vftech.fluxopro.project;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ProjectTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-23T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-24T10:00:00Z");

    private Project newProject() {
        Company company = new Company("Acme");
        return new Project(company, new Customer(company, "Client", null, null),
                null, null, "Website", null, null, T0);
    }

    @Test
    void newProjectStartsInPlanning() {
        Project project = newProject();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(project.getStatusChangedAt()).isEqualTo(T0);
    }

    @Test
    void movingForwardStampsTheStatusChangeDate() {
        Project project = newProject();
        project.changeStatus(ProjectStatus.IN_PROGRESS, T1);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getStatusChangedAt()).isEqualTo(T1);
    }

    @Test
    void skippingAStageIsRejected() {
        Project project = newProject();
        assertThatThrownBy(() -> project.changeStatus(ProjectStatus.DELIVERED, T1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PLANNING");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNING);
    }

    @Test
    void openProjectCannotBeArchived() {
        Project project = newProject();
        assertThatThrownBy(() -> project.archive(T1)).isInstanceOf(BusinessRuleException.class);
        assertThat(project.isArchived()).isFalse();
    }

    @Test
    void deliveredProjectCanBeArchivedAndArchiveIsIdempotent() {
        Project project = newProject();
        project.changeStatus(ProjectStatus.IN_PROGRESS, T1);
        project.changeStatus(ProjectStatus.DELIVERED, T1);

        project.archive(T1);
        project.archive(T2);

        assertThat(project.getArchivedAt()).isEqualTo(T1);
    }

    @Test
    void archivedProjectCannotBeEditedOrMoved() {
        Project project = newProject();
        project.changeStatus(ProjectStatus.CANCELLED, T1);
        project.archive(T1);

        assertThatThrownBy(() -> project.update("New", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Archived projects");
        assertThatThrownBy(() -> project.changeStatus(ProjectStatus.IN_PROGRESS, T2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Archived projects");
    }
}
