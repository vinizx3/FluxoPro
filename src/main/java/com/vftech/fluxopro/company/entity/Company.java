package com.vftech.fluxopro.company.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "stalled_after_days", nullable = false)
    private int stalledAfterDays = 7;

    protected Company() {
        // required by JPA
    }

    public Company(String name) {
        this.name = name;
    }

    public int getStalledAfterDays() { return stalledAfterDays; }

    public void changeStalledAfterDays(int days) {
        if (days < 1 || days > 90) {
            throw new IllegalArgumentException("stalledAfterDays must be between 1 and 90");
        }
        this.stalledAfterDays = days;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
}
