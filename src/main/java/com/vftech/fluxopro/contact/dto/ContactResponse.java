package com.vftech.fluxopro.contact.dto;

import com.vftech.fluxopro.contact.entity.ContactKind;
import com.vftech.fluxopro.contact.entity.OpportunityContact;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        ContactKind kind,
        String note,
        UUID authorId,
        String authorName,
        Instant contactedAt) {

    public static ContactResponse from(OpportunityContact c) {
        return new ContactResponse(
                c.getId(),
                c.getKind(),
                c.getNote(),
                c.getAuthor().getId(),
                c.getAuthor().getName(),
                c.getContactedAt());
    }
}
