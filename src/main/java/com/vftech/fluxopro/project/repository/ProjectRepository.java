package com.vftech.fluxopro.project.repository;

import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = {"customer", "owner"})
    Optional<Project> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsBySourceOpportunityId(UUID opportunityId);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Project> findByCompanyIdAndArchivedAtIsNull(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Project> findByCompanyIdAndArchivedAtIsNullAndStatus(
            UUID companyId, ProjectStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Project> findByCompanyIdAndOwnerIdAndArchivedAtIsNull(
            UUID companyId, UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Project> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatus(
            UUID companyId, UUID ownerId, ProjectStatus status, Pageable pageable);

    boolean existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
            UUID customerId, UUID companyId, Collection<ProjectStatus> statuses);
}
