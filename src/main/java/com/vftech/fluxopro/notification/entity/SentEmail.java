package com.vftech.fluxopro.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sent_email")
public class SentEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    protected SentEmail() {
    }

    public SentEmail(UUID eventId, UUID userId, Instant sentAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.sentAt = sentAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getUserId() { return userId; }
    public Instant getSentAt() { return sentAt; }
}
