package com.vftech.fluxopro.opportunity;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OpportunityStatus from, OpportunityStatus to) {
        super("Cannot move opportunity from " + from + " to " + to);
    }
}
