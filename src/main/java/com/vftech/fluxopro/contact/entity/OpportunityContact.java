package com.vftech.fluxopro.contact.entity;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "opportunity_contact")
public class OpportunityContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, updatable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ContactKind kind;

    @Column(nullable = false, updatable = false, length = 1000)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant contactedAt;

    protected OpportunityContact() {
    }

    public OpportunityContact(Company company, Opportunity opportunity, User author,
                              ContactKind kind, String note, Instant contactedAt) {
        this.company = company;
        this.opportunity = opportunity;
        this.author = author;
        this.kind = kind;
        this.note = note;
        this.contactedAt = contactedAt;
    }

    public UUID getId() { return id; }
    public Company getCompany() { return company; }
    public Opportunity getOpportunity() { return opportunity; }
    public User getAuthor() { return author; }
    public ContactKind getKind() { return kind; }
    public String getNote() { return note; }
    public Instant getContactedAt() { return contactedAt; }
}
