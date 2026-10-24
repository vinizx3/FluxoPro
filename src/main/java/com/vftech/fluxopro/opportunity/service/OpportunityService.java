package com.vftech.fluxopro.opportunity.service;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.audit.service.ChangeSet;
import com.vftech.fluxopro.common.concurrency.VersionCheck;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.opportunity.dto.*;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.event.OpportunityWonEvent;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
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
    private final ApplicationEventPublisher events;
    private final AuditRecorder audit;

    public OpportunityService(OpportunityRepository opportunities, CustomerRepository customers, CompanyRepository companies, UserRepository users, Clock clock, ApplicationEventPublisher events, AuditRecorder audit) {
        this.opportunities = opportunities;
        this.customers = customers;
        this.companies = companies;
        this.users = users;
        this.clock = clock;
        this.events = events;
        this.audit = audit;
    }

    @Transactional
    public OpportunityResponse create(AuthenticatedUser user, OpportunityRequest request) {
        Customer customer = customers.findByIdAndCompanyIdAndArchivedAtIsNull(request.customerId(), user.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Company company = companies.getReferenceById(user.companyId());
        User owner = users.getReferenceById(user.id());

        Opportunity opportunity = opportunities.save(new Opportunity(
                company, customer, owner, request.title().trim(), request.amount(), Instant.now(clock)));
        audit.record(user, AuditEntityType.OPPORTUNITY, opportunity.getId(), AuditAction.CREATED,
                ChangeSet.create()
                        .track("title", null, opportunity.getTitle())
                        .track("amount", null, opportunity.getAmount())
                        .track("customerId", null, request.customerId())
                        .build());
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

        String title = request.title().trim();
        List<FieldChange> changes = ChangeSet.create()
                .track("title", opportunity.getTitle(), title)
                .track("amount", opportunity.getAmount(), request.amount())
                .build();

        opportunity.update(title, request.amount());
        opportunities.saveAndFlush(opportunity);
        audit.record(user, AuditEntityType.OPPORTUNITY, opportunity.getId(), AuditAction.UPDATED, changes);
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public OpportunityResponse move(AuthenticatedUser user, UUID id, MoveStatusRequest request) {
        Opportunity opportunity = findVisible(user, id);
        VersionCheck.ensure(opportunity.getVersion(), request.version());

        OpportunityStatus statusBefore = opportunity.getStatus();
        String lostReasonBefore = opportunity.getLostReason();

        opportunity.changeStatus(request.status(), request.lostReason(), Instant.now(clock));
        opportunities.saveAndFlush(opportunity);

        audit.record(user, AuditEntityType.OPPORTUNITY, opportunity.getId(), AuditAction.STATUS_CHANGED,
                ChangeSet.create()
                        .track("status", statusBefore, opportunity.getStatus())
                        .track("lostReason", lostReasonBefore, opportunity.getLostReason())
                        .build());

        if (opportunity.getStatus() == OpportunityStatus.WON) {
            events.publishEvent(new OpportunityWonEvent(
                    opportunity.getId(),
                    opportunity.getCompany().getId(),
                    opportunity.getCustomer().getId(),
                    opportunity.getOwner().getId(),
                    user.id(),
                    opportunity.getTitle()));
        }
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public void archive(AuthenticatedUser user, UUID id) {
        Opportunity opportunity = findVisible(user, id);
        boolean alreadyArchived = opportunity.isArchived();
        opportunity.archive(Instant.now(clock));
        if (!alreadyArchived) {
            audit.record(user, AuditEntityType.OPPORTUNITY, id, AuditAction.ARCHIVED, List.of());
        }
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_STALLED_DAYS = 365;

    @Transactional(readOnly = true)
    public PageResponse<StalledOpportunityResponse> stalled(AuthenticatedUser user, int days, int page, int size) {
        int safeDays = Math.min(Math.max(days, 1), MAX_STALLED_DAYS);
        Instant now = Instant.now(clock);
        Instant limit = now.minus(Duration.ofDays(safeDays));

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("statusChangedAt").ascending().and(Sort.by("id").ascending()));

        Page<Opportunity> result = user.isOwner()
                ? opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                user.companyId(), OPEN_STATUSES, limit, pageable)
                : opportunities.findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                user.companyId(), user.id(), OPEN_STATUSES, limit, pageable);

        return PageResponse.of(result, o -> StalledOpportunityResponse.from(o, now));
    }

    private Opportunity findVisible(AuthenticatedUser user, UUID id) {
        return opportunities.findByIdAndCompanyId(id, user.companyId())
                .filter(o -> user.isOwner() || o.getOwner().getId().equals(user.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
    }
}
