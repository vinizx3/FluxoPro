package com.vftech.fluxopro.opportunity.service;

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
    private final Clock clock;

    public OpportunityWonListener(ProjectRepository projects, CompanyRepository companies,
                                  CustomerRepository customers, UserRepository users,
                                  OpportunityRepository opportunities, Clock clock) {
        this.projects = projects;
        this.companies = companies;
        this.customers = customers;
        this.users = users;
        this.opportunities = opportunities;
        this.clock = clock;
    }

    @EventListener
    public void onOpportunityWon(OpportunityWonEvent event) {
        if (projects.existsBySourceOpportunityId(event.opportunityId())) {
            return;
        }
        projects.save(new Project(
                companies.getReferenceById(event.companyId()),
                customers.getReferenceById(event.customerId()),
                opportunities.getReferenceById(event.opportunityId()),
                users.getReferenceById(event.ownerId()),
                event.title(),
                null,
                null,
                Instant.now(clock)));
    }
}
