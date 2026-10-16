package com.vftech.fluxopro.opportunity.entity;

import com.vftech.fluxopro.exception.BusinessRuleException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.exception.InvalidStatusTransitionException;
import com.vftech.fluxopro.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "opportunity")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OpportunityStatus status;

    @Column(length = 255)
    private String lostReason;

    @Column(nullable = false)
    private Instant statusChangedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Opportunity() {
    }

    public Opportunity(Company company, Customer customer, User owner,
                       String title, BigDecimal amount, Instant now) {
        this.company = company;
        this.customer = customer;
        this.owner = owner;
        this.title = title;
        this.amount = amount;
        this.status = OpportunityStatus.LEAD;
        this.statusChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeStatus(OpportunityStatus target, String lostReason, Instant now) {
        if (!status.canMoveTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        if (target == OpportunityStatus.LOST) {
            if (lostReason == null || lostReason.isBlank()) {
                throw new BusinessRuleException("A reason is required to mark an opportunity as lost");
            }
            this.lostReason = lostReason.trim();
        }
        this.status = target;
        this.statusChangedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public Customer getCustomer() { return customer; }
    public User getOwner() { return owner; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public OpportunityStatus getStatus() { return status; }
    public String getLostReason() { return lostReason; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
