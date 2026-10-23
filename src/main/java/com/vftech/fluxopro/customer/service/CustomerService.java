package com.vftech.fluxopro.customer.service;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.audit.service.ChangeSet;
import com.vftech.fluxopro.common.concurrency.VersionCheck;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.dto.CustomerRequest;
import com.vftech.fluxopro.customer.dto.CustomerResponse;
import com.vftech.fluxopro.customer.dto.CustomerUpdateRequest;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import com.vftech.fluxopro.project.repository.ProjectRepository;
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
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerRepository customers;
    private final CompanyRepository companies;
    private final OpportunityRepository opportunities;
    private final ProjectRepository projectRepository;
    private final AuditRecorder audit;
    private final Clock clock;

    public CustomerService(CustomerRepository customers, CompanyRepository companies, OpportunityRepository opportunities, ProjectRepository projectRepository, AuditRecorder audit ,Clock clock) {
        this.customers = customers;
        this.companies = companies;
        this.opportunities = opportunities;
        this.projectRepository = projectRepository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public CustomerResponse create(AuthenticatedUser user, CustomerRequest request) {
        Company company = companies.getReferenceById(user.companyId());
        Customer customer = customers.save(new Customer(
                company,
                request.name().trim(),
                blankToNull(request.email()),
                blankToNull(request.phone())));
        audit.record(user, AuditEntityType.CUSTOMER, customer.getId(), AuditAction.CREATED,
                ChangeSet.create()
                        .track("name", null, customer.getName())
                        .track("email", null, customer.getEmail())
                        .track("phone", null, customer.getPhone())
                        .build());
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(AuthenticatedUser user, String search, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("name").ascending().and(Sort.by("id").ascending()));

        Page<Customer> result = (search == null || search.isBlank())
                ? customers.findByCompanyIdAndArchivedAtIsNull(user.companyId(), pageable)
                : customers.findByCompanyIdAndArchivedAtIsNullAndNameContainingIgnoreCase(
                user.companyId(), search.trim(), pageable);
        return PageResponse.of(result, CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(AuthenticatedUser user, UUID id) {
        return CustomerResponse.from(find(user, id));
    }

    @Transactional
    public CustomerResponse update(AuthenticatedUser user, UUID id, CustomerUpdateRequest request) {
        Customer customer = find(user, id);
        VersionCheck.ensure(customer.getVersion(), request.version());

        String name = request.name().trim();
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());
        List<FieldChange> changes = ChangeSet.create()
                .track("name", customer.getName(), name)
                .track("email", customer.getEmail(), email)
                .track("phone", customer.getPhone(), phone)
                .build();

        customer.update(name, email, phone);
        customers.saveAndFlush(customer);
        audit.record(user, AuditEntityType.CUSTOMER, customer.getId(), AuditAction.UPDATED, changes);
        return CustomerResponse.from(customer);
    }


    @Transactional
    public void archive(AuthenticatedUser user, UUID id) {
        Customer customer = find(user, id);

        boolean hasOpenOpportunities = opportunities.existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
                id, user.companyId(), OpportunityStatus.openStatuses());
        if (hasOpenOpportunities) {
            throw new BusinessRuleException("Customer has open opportunities and cannot be archived");
        }
        boolean hasOpenProjects = projectRepository.existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
                id, user.companyId(), ProjectStatus.openStatuses());
        if (hasOpenProjects) {
            throw new BusinessRuleException("Customer has open projects and cannot be archived");
        }

        boolean alreadyArchived = customer.isArchived();
        customer.archive(Instant.now(clock));
        if (!alreadyArchived) {
            audit.record(user, AuditEntityType.CUSTOMER, id, AuditAction.ARCHIVED, List.of());
        }
    }

    private Customer find(AuthenticatedUser user, UUID id) {
        return customers.findByIdAndCompanyId(id, user.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
