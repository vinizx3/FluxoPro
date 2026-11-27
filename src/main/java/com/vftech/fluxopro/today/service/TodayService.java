package com.vftech.fluxopro.today.service;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.today.dto.TodayResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class TodayService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_WITHOUT_NEXT_STEP = 200;

    private final OpportunityRepository opportunities;
    private final Clock clock;

    public TodayService(OpportunityRepository opportunities, Clock clock) {
        this.opportunities = opportunities;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TodayResponse build(AuthenticatedUser user) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<OpportunityStatus> open = OpportunityStatus.openStatuses();

        List<Opportunity> due = user.isOwner()
                ? opportunities.findDue(user.companyId(), open, today)
                : opportunities.findDueByOwner(user.companyId(), user.id(), open, today);
        List<Opportunity> without = user.isOwner()
                ? opportunities.findWithoutNextStep(user.companyId(), open)
                : opportunities.findWithoutNextStepByOwner(user.companyId(), user.id(), open);

        return new TodayResponse(
                today,
                due.stream().filter(o -> o.getNextStepAt().isBefore(today))
                        .map(o -> TodayResponse.Item.from(o, today, ZONE)).toList(),
                due.stream().filter(o -> o.getNextStepAt().isEqual(today))
                        .map(o -> TodayResponse.Item.from(o, today, ZONE)).toList(),
                without.stream().limit(MAX_WITHOUT_NEXT_STEP)
                        .map(o -> TodayResponse.Item.from(o, today, ZONE)).toList());
    }
}
