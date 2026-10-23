package com.vftech.fluxopro.opportunity.event;

import java.util.UUID;

public record OpportunityWonEvent(
        UUID opportunityId,
        UUID companyId,
        UUID customerId,
        UUID ownerId,
        UUID actorId,
        String title) {
}
