package com.vftech.fluxopro.opportunity.service;

import com.vftech.fluxopro.common.concurrency.VersionCheck;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.opportunity.dto.MoveStatusRequest;
import com.vftech.fluxopro.opportunity.dto.OpportunityRequest;
import com.vftech.fluxopro.opportunity.dto.OpportunityResponse;
import com.vftech.fluxopro.opportunity.dto.OpportunityUpdateRequest;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class OpportunityService {

    private static final List<OpportunityStatus> OPEN_STATUSES = Arrays.stream(OpportunityStatus.values())
            .filter(OpportunityStatus::isOpen)
            .toList();

    private final OpportunityRepository opportunities;
    private final CustomerRepository customers;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final Clock clock;

    public OpportunityService(OpportunityRepository opportunities,
                              CustomerRepository customers,
                              CompanyRepository companies,
                              UserRepository users,
                              Clock clock) {
        this.opportunities = opportunities;
        this.customers = customers;
        this.companies = companies;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public OpportunityResponse create(AuthenticatedUser user, OpportunityRequest request) {
        Customer customer = customers.findByIdAndCompanyIdAndArchivedAtIsNull(request.customerId(), user.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Company company = companies.getReferenceById(user.companyId());
        User owner = users.getReferenceById(user.id());

        Opportunity opportunity = opportunities.save(new Opportunity(
                company, customer, owner, request.title().trim(), request.amount(), Instant.now(clock)));
        return OpportunityResponse.from(opportunity);
    }

    @Transactional(readOnly = true)
    public Map<OpportunityStatus, List<OpportunityResponse>> board(AuthenticatedUser user) {
        List<Opportunity> open = user.isOwner()
                ? opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInOrderByStatusChangedAtAsc(
                user.companyId(), OPEN_STATUSES)
                : opportunities.findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInOrderByStatusChangedAtAsc(
                user.companyId(), user.id(), OPEN_STATUSES);

        Map<OpportunityStatus, List<OpportunityResponse>> board = new EnumMap<>(OpportunityStatus.class);
        OPEN_STATUSES.forEach(status -> board.put(status, new ArrayList<>()));
        open.forEach(o -> board.get(o.getStatus()).add(OpportunityResponse.from(o)));
        return board;
    }

    @Transactional(readOnly = true)
    public OpportunityResponse get(AuthenticatedUser user, UUID id) {
        return OpportunityResponse.from(findVisible(user, id));
    }

    @Transactional
    public OpportunityResponse update(AuthenticatedUser user, UUID id, OpportunityUpdateRequest request) {
        Opportunity opportunity = findVisible(user, id);
        VersionCheck.ensure(opportunity.getVersion(), request.version());
        opportunity.update(request.title().trim(), request.amount());
        opportunities.saveAndFlush(opportunity);
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public OpportunityResponse move(AuthenticatedUser user, UUID id, MoveStatusRequest request) {
        Opportunity opportunity = findVisible(user, id);
        VersionCheck.ensure(opportunity.getVersion(), request.version());
        opportunity.changeStatus(request.status(), request.lostReason(), Instant.now(clock));
        opportunities.saveAndFlush(opportunity);
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public void archive(AuthenticatedUser user, UUID id) {
        findVisible(user, id).archive(Instant.now(clock));
    }

    private Opportunity findVisible(AuthenticatedUser user, UUID id) {
        return opportunities.findByIdAndCompanyId(id, user.companyId())
                .filter(o -> user.isOwner() || o.getOwner().getId().equals(user.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
    }
}
