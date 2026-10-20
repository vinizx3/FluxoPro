package com.vftech.fluxopro.customer.entity;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.exception.BusinessRuleException;
import jakarta.persistence.*;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    public Customer(Company company, String name, String email, String phone) {
        this.company = company;
        this.name = name;
        this.email = email;
        this.phone = phone;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Version
    private Long version;

    private Instant archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Instant getArchivedAt() { return archivedAt; }

    public void update(String name, String email, String phone) {
        ensureNotArchived();
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void archive(Instant now) {
        if (archivedAt == null) {
            this.archivedAt = now;
        }
    }

    private void ensureNotArchived() {
        if (archivedAt != null) {
            throw new BusinessRuleException("Archived customers cannot be changed");
        }
    }
}
