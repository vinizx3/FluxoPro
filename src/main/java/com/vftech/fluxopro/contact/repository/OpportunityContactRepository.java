package com.vftech.fluxopro.contact.repository;

import com.vftech.fluxopro.contact.entity.OpportunityContact;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpportunityContactRepository extends JpaRepository<OpportunityContact, UUID> {

    @EntityGraph(attributePaths = "author")
    List<OpportunityContact> findTop100ByOpportunityIdAndCompanyIdOrderByContactedAtDesc(
            UUID opportunityId, UUID companyId);
}
