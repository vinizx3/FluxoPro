package com.vftech.fluxopro.notification.entity;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, updatable = false)
    private Opportunity opportunity;

    @Column(nullable = false, updatable = false)
    private Instant stalledSince;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant readAt;

    protected Notification() {
    }

    public Notification(Company company, User recipient, NotificationType type,
                        Opportunity opportunity, Instant stalledSince, Instant now) {
        this.company = company;
        this.recipient = recipient;
        this.type = type;
        this.opportunity = opportunity;
        this.stalledSince = stalledSince;
        this.createdAt = now;
    }

    public void markAsRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public User getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public Opportunity getOpportunity() { return opportunity; }
    public Instant getStalledSince() { return stalledSince; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
