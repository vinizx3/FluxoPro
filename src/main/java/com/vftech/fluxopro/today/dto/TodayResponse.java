package com.vftech.fluxopro.today.dto;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record TodayResponse(
        LocalDate date,
        List<Item> overdue,
        List<Item> dueToday,
        List<Item> withoutNextStep) {

    public record Item(
            UUID id,
            String title,
            UUID customerId,
            String customerName,
            String customerPhone,
            UUID ownerId,
            String ownerName,
            BigDecimal amount,
            OpportunityStatus status,
            LocalDate nextStepAt,
            String nextStepNote,
            long daysOverdue,
            long daysSinceLastActivity,
            Long version) {

        public static Item from(Opportunity o, LocalDate today, ZoneId zone) {
            long overdue = o.getNextStepAt() == null ? 0
                    : Math.max(0, ChronoUnit.DAYS.between(o.getNextStepAt(), today));
            long idle = Math.max(0, ChronoUnit.DAYS.between(
                    o.getLastActivityAt().atZone(zone).toLocalDate(), today));
            return new Item(
                    o.getId(), o.getTitle(),
                    o.getCustomer().getId(), o.getCustomer().getName(), o.getCustomer().getPhone(),
                    o.getOwner().getId(), o.getOwner().getName(),
                    o.getAmount(), o.getStatus(),
                    o.getNextStepAt(), o.getNextStepNote(),
                    overdue, idle, o.getVersion());
        }
    }
}
