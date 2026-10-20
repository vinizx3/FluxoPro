package com.vftech.fluxopro.common.exception;

import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

public class InvalidStatusTransitionException extends BusinessRuleException {
    public InvalidStatusTransitionException(OpportunityStatus from, OpportunityStatus to) {
        super("Cannot move opportunity from " + from + " to " + to);
    }
}
