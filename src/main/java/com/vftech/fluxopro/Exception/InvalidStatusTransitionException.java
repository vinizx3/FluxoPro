package com.vftech.fluxopro.Exception;

import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OpportunityStatus from, OpportunityStatus to) {
        super("Cannot move opportunity from " + from + " to " + to);
    }
}
