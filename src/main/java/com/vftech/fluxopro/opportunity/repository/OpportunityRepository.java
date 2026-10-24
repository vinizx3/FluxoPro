package com.vftech.fluxopro.opportunity.repository;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    @EntityGraph(attributePaths = {"customer", "owner"})
    Optional<Opportunity> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
            UUID customerId, UUID companyId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInOrderByStatusChangedAtAsc(
            UUID companyId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInOrderByStatusChangedAtAsc(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant limit, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses, Instant limit, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant limit);
}
