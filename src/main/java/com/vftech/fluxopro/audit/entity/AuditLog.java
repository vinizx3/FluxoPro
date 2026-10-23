package com.vftech.fluxopro.audit.entity;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditEntityType entityType;

    @Column(nullable = false, updatable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditAction action;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "audit_log_change", joinColumns = @JoinColumn(name = "audit_log_id"))
    @BatchSize(size = 50)
    private List<FieldChange> changes = new ArrayList<>();

    protected AuditLog() {
    }

    public AuditLog(Company company, User actor, AuditEntityType entityType, UUID entityId,
                    AuditAction action, List<FieldChange> changes, Instant occurredAt) {
        this.company = company;
        this.actor = actor;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.changes = new ArrayList<>(changes);
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public User getActor() { return actor; }
    public AuditEntityType getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public AuditAction getAction() { return action; }
    public Instant getOccurredAt() { return occurredAt; }
    public List<FieldChange> getChanges() { return changes; }
}
