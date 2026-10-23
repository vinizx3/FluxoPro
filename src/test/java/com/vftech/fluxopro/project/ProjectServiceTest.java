package com.vftech.fluxopro.project;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.common.exception.ConflictException;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.project.dto.ProjectCreateRequest;
import com.vftech.fluxopro.project.dto.ProjectResponse;
import com.vftech.fluxopro.project.dto.ProjectStatusRequest;
import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import com.vftech.fluxopro.project.repository.ProjectRepository;
import com.vftech.fluxopro.project.service.ProjectService;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

    @Mock
    ProjectRepository projects;
    @Mock
    CustomerRepository customers;
    @Mock
    CompanyRepository companies;
    @Mock
    UserRepository users;
    @Mock
    AuditRecorder audit;

    @Captor ArgumentCaptor<List<FieldChange>> changes;

    private final UUID companyId = UUID.randomUUID();
    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projects, customers, companies, users, audit, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    private AuthenticatedUser seller() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.SELLER);
    }

    private Project project(long version, User projectOwner) {
        Company company = new Company("Acme");
        Project project = new Project(company, new Customer(company, "Client", null, null),
                null, projectOwner, "Website", null, null, Instant.parse("2026-10-01T00:00:00Z"));
        ReflectionTestUtils.setField(project, "version", version);
        return project;
    }

    @Test
    void createRejectsCustomerFromAnotherCompany() {
        UUID customerId = UUID.randomUUID();
        when(customers.findByIdAndCompanyIdAndArchivedAtIsNull(customerId, companyId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(owner(),
                new ProjectCreateRequest(customerId, "Website", null, null)))
                .isInstanceOf(ResponseStatusException.class);

        verify(projects, never()).save(any());
    }

    @Test
    void createStartsInPlanningWithTrimmedName() {
        UUID customerId = UUID.randomUUID();
        AuthenticatedUser user = owner();
        Company company = new Company("Acme");
        when(customers.findByIdAndCompanyIdAndArchivedAtIsNull(customerId, companyId))
                .thenReturn(Optional.of(new Customer(company, "Client", null, null)));
        when(companies.getReferenceById(companyId)).thenReturn(company);
        when(users.getReferenceById(user.id())).thenReturn(mock(User.class));
        when(projects.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = service.create(user,
                new ProjectCreateRequest(customerId, "  Website  ", " ", null));

        assertThat(response.name()).isEqualTo("Website");
        assertThat(response.description()).isNull();
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNING);
    }

    @Test
    void moveWithStaleVersionIsRejectedAndChangesNothing() {
        UUID id = UUID.randomUUID();
        Project project = project(4L, mock(User.class));
        when(projects.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.move(owner(), id,
                new ProjectStatusRequest(ProjectStatus.IN_PROGRESS, 3L)))
                .isInstanceOf(ConflictException.class);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNING);
        verify(projects, never()).saveAndFlush(any());
    }

    @Test
    void moveStampsTheDateWithTheClock() {
        UUID id = UUID.randomUUID();
        Project project = project(2L, mock(User.class));
        when(projects.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(project));

        ProjectResponse response = service.move(owner(), id,
                new ProjectStatusRequest(ProjectStatus.IN_PROGRESS, 2L));

        assertThat(response.status()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getStatusChangedAt()).isEqualTo(NOW);
        verify(projects).saveAndFlush(project);
    }

    @Test
    void sellerCannotSeeProjectOfAnotherSeller() {
        UUID id = UUID.randomUUID();
        User otherSeller = mock(User.class);
        when(otherSeller.getId()).thenReturn(UUID.randomUUID());
        when(projects.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(project(0L, otherSeller)));

        assertThatThrownBy(() -> service.get(seller(), id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sellerListQueriesOnlyTheirOwnProjects() {
        AuthenticatedUser user = seller();
        when(projects.findByCompanyIdAndOwnerIdAndArchivedAtIsNull(
                eq(companyId), eq(user.id()), any(Pageable.class))).thenReturn(Page.empty());

        service.list(user, null, 0, 20);

        verify(projects).findByCompanyIdAndOwnerIdAndArchivedAtIsNull(
                eq(companyId), eq(user.id()), any(Pageable.class));
    }

    @Test
    void moveIsAuditedAsAStatusChange() {
        UUID id = UUID.randomUUID();
        AuthenticatedUser actor = owner();
        when(projects.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(project(2L, mock(User.class))));

        service.move(actor, id, new ProjectStatusRequest(ProjectStatus.IN_PROGRESS, 2L));

        verify(audit).record(eq(actor), eq(AuditEntityType.PROJECT), any(),
                eq(AuditAction.STATUS_CHANGED), changes.capture());
        assertThat(changes.getValue()).containsExactly(new FieldChange("status", "PLANNING", "IN_PROGRESS"));
    }

    @Test
    void rejectedMoveIsNotAudited() {
        UUID id = UUID.randomUUID();
        when(projects.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(project(4L, mock(User.class))));

        assertThatThrownBy(() -> service.move(owner(), id,
                new ProjectStatusRequest(ProjectStatus.IN_PROGRESS, 3L)))
                .isInstanceOf(ConflictException.class);

        verify(audit, never()).record(any(AuthenticatedUser.class), any(), any(), any(), any());
    }

    @Test
    void createIsAuditedWithItsInitialValues() {
        UUID customerId = UUID.randomUUID();
        AuthenticatedUser actor = owner();
        Company company = new Company("Acme");
        when(customers.findByIdAndCompanyIdAndArchivedAtIsNull(customerId, companyId))
                .thenReturn(Optional.of(new Customer(company, "Client", null, null)));
        when(companies.getReferenceById(companyId)).thenReturn(company);
        when(users.getReferenceById(actor.id())).thenReturn(mock(User.class));
        when(projects.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(actor, new ProjectCreateRequest(customerId, "Website", null, null));

        verify(audit).record(eq(actor), eq(AuditEntityType.PROJECT), any(),
                eq(AuditAction.CREATED), changes.capture());
        assertThat(changes.getValue()).contains(new FieldChange("name", null, "Website"));
    }

    @Test
    void listClampsPageSizeAndNegativePage() {
        when(projects.findByCompanyIdAndArchivedAtIsNull(eq(companyId), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(owner(), null, -5, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projects).findByCompanyIdAndArchivedAtIsNull(eq(companyId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
