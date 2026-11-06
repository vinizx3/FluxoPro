package com.vftech.fluxopro.dashboard.service;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.dashboard.dto.DashboardResponse;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DashboardService {

    private final OpportunityRepository opportunities;
    private final Clock clock;

    public DashboardService(OpportunityRepository opportunities, Clock clock) {
        this.opportunities = opportunities;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build(AuthenticatedUser user, int days) {
        if (!user.isOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can see the dashboard");
        }
        int period = Math.min(365, Math.max(7, days));
        Instant now = clock.instant();

        List<Opportunity> open = opportunities.findByCompanyIdAndStatusIn(
                user.companyId(), OpportunityStatus.openStatuses());
        List<Opportunity> closed = opportunities.findByCompanyIdAndStatusInAndStatusChangedAtAfter(
                user.companyId(), List.of(OpportunityStatus.WON, OpportunityStatus.LOST),
                now.minus(period, ChronoUnit.DAYS));

        return com.vftech.fluxopro.dashboard.service.DashboardCalculator.calculate(open, closed, now, period);
    }
}
