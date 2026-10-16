package com.vftech.fluxopro.opportunity.repository;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    @EntityGraph(attributePaths = {"customer", "owner"})
    Optional<Opportunity> findByIdAndCompanyId(UUID id, UUID companyId);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByCompanyIdAndStatusInOrderByStatusChangedAtAsc(
            UUID companyId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByCompanyIdAndOwnerIdAndStatusInOrderByStatusChangedAtAsc(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses);
}
