package com.vftech.fluxopro.opportunity.repository;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
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
    List<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
            UUID companyId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInAndLastActivityAtBefore(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant limit, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInAndLastActivityAtBefore(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses, Instant limit, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant limit);

    @EntityGraph(attributePaths = "owner")
    List<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusInAndLastActivityAtBefore(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant limit);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndArchivedAtIsNullAndStatusIn(
            UUID companyId, Collection<OpportunityStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "owner"})
    Page<Opportunity> findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusIn(
            UUID companyId, UUID ownerId, Collection<OpportunityStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Opportunity> findByCompanyIdAndStatusIn(UUID companyId, Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = "owner")
    List<Opportunity> findByCompanyIdAndStatusInAndStatusChangedAtAfter(
            UUID companyId, Collection<OpportunityStatus> statuses, Instant since);

    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("""
        select o from Opportunity o
        where o.company.id = :companyId and o.archivedAt is null
          and o.status in :statuses and o.nextStepAt <= :until
        order by o.nextStepAt asc, o.lastActivityAt asc
        """)
    List<Opportunity> findDue(@Param("companyId") UUID companyId,
                              @Param("statuses") Collection<OpportunityStatus> statuses,
                              @Param("until") LocalDate until);

    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("""
        select o from Opportunity o
        where o.company.id = :companyId and o.owner.id = :ownerId and o.archivedAt is null
          and o.status in :statuses and o.nextStepAt <= :until
        order by o.nextStepAt asc, o.lastActivityAt asc
        """)
    List<Opportunity> findDueByOwner(@Param("companyId") UUID companyId,
                                     @Param("ownerId") UUID ownerId,
                                     @Param("statuses") Collection<OpportunityStatus> statuses,
                                     @Param("until") LocalDate until);

    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("""
        select o from Opportunity o
        where o.company.id = :companyId and o.archivedAt is null
          and o.status in :statuses and o.nextStepAt is null
        order by o.lastActivityAt asc
        """)
    List<Opportunity> findWithoutNextStep(@Param("companyId") UUID companyId,
                                          @Param("statuses") Collection<OpportunityStatus> statuses);

    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("""
        select o from Opportunity o
        where o.company.id = :companyId and o.owner.id = :ownerId and o.archivedAt is null
          and o.status in :statuses and o.nextStepAt is null
        order by o.lastActivityAt asc
        """)
    List<Opportunity> findWithoutNextStepByOwner(@Param("companyId") UUID companyId,
                                                 @Param("ownerId") UUID ownerId,
                                                 @Param("statuses") Collection<OpportunityStatus> statuses);
}
