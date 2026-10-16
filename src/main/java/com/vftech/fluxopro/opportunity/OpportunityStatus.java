package com.vftech.fluxopro.opportunity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OpportunityStatus {
    LEAD, QUALIFICATION, PROPOSAL, NEGOTIATION, WON, LOST;

    private static final Map<OpportunityStatus, Set<OpportunityStatus>> ALLOWED = Map.of(
            LEAD, EnumSet.of(QUALIFICATION, LOST),
            QUALIFICATION, EnumSet.of(LEAD, PROPOSAL, LOST),
            PROPOSAL, EnumSet.of(QUALIFICATION, NEGOTIATION, LOST),
            NEGOTIATION, EnumSet.of(PROPOSAL, WON, LOST),
            WON, EnumSet.noneOf(OpportunityStatus.class),
            LOST, EnumSet.noneOf(OpportunityStatus.class));

    public boolean canMoveTo(OpportunityStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public boolean isOpen() {
        return this != WON && this != LOST;
    }
}
