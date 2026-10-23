package com.vftech.fluxopro.opportunity.service;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.audit.service.ChangeSet;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.opportunity.event.OpportunityWonEvent;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.repository.ProjectRepository;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class OpportunityWonListener {

    private final ProjectRepository projects;
    private final CompanyRepository companies;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final OpportunityRepository opportunities;
    private final AuditRecorder audit;
    private final Clock clock;

    public OpportunityWonListener(ProjectRepository projects, CompanyRepository companies, CustomerRepository customers, UserRepository users, OpportunityRepository opportunities, AuditRecorder audit, Clock clock) {
        this.projects = projects;
        this.companies = companies;
        this.customers = customers;
        this.users = users;
        this.opportunities = opportunities;
        this.audit = audit;
        this.clock = clock;
    }

    @EventListener
    public void onOpportunityWon(OpportunityWonEvent event) {
        if (projects.existsBySourceOpportunityId(event.opportunityId())) {
            return;
        }
        Project project = projects.save(new Project(
                companies.getReferenceById(event.companyId()),
                customers.getReferenceById(event.customerId()),
                opportunities.getReferenceById(event.opportunityId()),
                users.getReferenceById(event.ownerId()),
                event.title(),
                null,
                null,
                Instant.now(clock)));

        audit.record(event.companyId(), event.actorId(), AuditEntityType.PROJECT, project.getId(),
                AuditAction.CREATED,
                ChangeSet.create()
                        .track("name", null, project.getName())
                        .track("sourceOpportunityId", null, event.opportunityId())
                        .build());
    }
}
