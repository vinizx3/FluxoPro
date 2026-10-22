package com.vftech.fluxopro.project.service;

import com.vftech.fluxopro.common.concurrency.VersionCheck;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.project.dto.ProjectCreateRequest;
import com.vftech.fluxopro.project.dto.ProjectResponse;
import com.vftech.fluxopro.project.dto.ProjectStatusRequest;
import com.vftech.fluxopro.project.dto.ProjectUpdateRequest;
import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import com.vftech.fluxopro.project.repository.ProjectRepository;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProjectService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectRepository projects;
    private final CustomerRepository customers;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final Clock clock;

    public ProjectService(ProjectRepository projects, CustomerRepository customers,
                          CompanyRepository companies, UserRepository users, Clock clock) {
        this.projects = projects;
        this.customers = customers;
        this.companies = companies;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public ProjectResponse create(AuthenticatedUser user, ProjectCreateRequest request) {
        Customer customer = customers.findByIdAndCompanyIdAndArchivedAtIsNull(request.customerId(), user.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Project project = projects.save(new Project(
                companies.getReferenceById(user.companyId()),
                customer,
                null,
                users.getReferenceById(user.id()),
                request.name().trim(),
                blankToNull(request.description()),
                request.expectedDeliveryDate(),
                Instant.now(clock)));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(AuthenticatedUser user, ProjectStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<Project> result;
        if (user.isOwner()) {
            result = status == null
                    ? projects.findByCompanyIdAndArchivedAtIsNull(user.companyId(), pageable)
                    : projects.findByCompanyIdAndArchivedAtIsNullAndStatus(user.companyId(), status, pageable);
        } else {
            result = status == null
                    ? projects.findByCompanyIdAndOwnerIdAndArchivedAtIsNull(user.companyId(), user.id(), pageable)
                    : projects.findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatus(
                    user.companyId(), user.id(), status, pageable);
        }
        return PageResponse.of(result, ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(AuthenticatedUser user, UUID id) {
        return ProjectResponse.from(findVisible(user, id));
    }

    @Transactional
    public ProjectResponse update(AuthenticatedUser user, UUID id, ProjectUpdateRequest request) {
        Project project = findVisible(user, id);
        VersionCheck.ensure(project.getVersion(), request.version());
        project.update(request.name().trim(), blankToNull(request.description()), request.expectedDeliveryDate());
        projects.saveAndFlush(project);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse move(AuthenticatedUser user, UUID id, ProjectStatusRequest request) {
        Project project = findVisible(user, id);
        VersionCheck.ensure(project.getVersion(), request.version());
        project.changeStatus(request.status(), Instant.now(clock));
        projects.saveAndFlush(project);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void archive(AuthenticatedUser user, UUID id) {
        findVisible(user, id).archive(Instant.now(clock));
    }

    private Project findVisible(AuthenticatedUser user, UUID id) {
        return projects.findByIdAndCompanyId(id, user.companyId())
                .filter(p -> user.isOwner() || p.getOwner().getId().equals(user.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
