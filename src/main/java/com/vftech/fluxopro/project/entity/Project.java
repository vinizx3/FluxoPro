package com.vftech.fluxopro.project.entity;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_opportunity_id", updatable = false)
    private Opportunity sourceOpportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(nullable = false)
    private Instant statusChangedAt;

    private LocalDate expectedDeliveryDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant archivedAt;

    @Version
    private Long version;

    protected Project() {
    }

    public Project(Company company, Customer customer, Opportunity sourceOpportunity, User owner,
                   String name, String description, LocalDate expectedDeliveryDate, Instant now) {
        this.company = company;
        this.customer = customer;
        this.sourceOpportunity = sourceOpportunity;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.status = ProjectStatus.PLANNING;
        this.statusChangedAt = now;
        this.createdAt = now;
    }

    public void update(String name, String description, LocalDate expectedDeliveryDate) {
        ensureNotArchived();
        this.name = name;
        this.description = description;
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public void changeStatus(ProjectStatus target, Instant now) {
        ensureNotArchived();
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("Cannot move project from " + status + " to " + target);
        }
        this.status = target;
        this.statusChangedAt = now;
    }

    public void archive(Instant now) {
        if (archivedAt != null) {
            return;
        }
        if (status.isOpen()) {
            throw new BusinessRuleException("Only delivered or cancelled projects can be archived");
        }
        this.archivedAt = now;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    private void ensureNotArchived() {
        if (archivedAt != null) {
            throw new BusinessRuleException("Archived projects cannot be changed");
        }
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public Customer getCustomer() { return customer; }
    public Opportunity getSourceOpportunity() { return sourceOpportunity; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public Long getVersion() { return version; }
}
