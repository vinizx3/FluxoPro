package com.vftech.fluxopro.dashboard;

import com.vftech.fluxopro.dashboard.dto.DashboardResponse;
import com.vftech.fluxopro.dashboard.service.DashboardCalculator;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class DashboardCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private User seller(String name) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(UUID.randomUUID());
        lenient().when(user.getName()).thenReturn(name);
        return user;
    }

    private Opportunity open(User owner, String amount, int daysSinceActivity) {
        return new Opportunity(null, null, owner, "Deal", new BigDecimal(amount),
                NOW.minus(daysSinceActivity, ChronoUnit.DAYS));
    }

    private Opportunity won(User owner, String amount, int daysToWin) {
        Instant created = NOW.minus(daysToWin + 1, ChronoUnit.DAYS);
        Opportunity o = new Opportunity(null, null, owner, "Deal", new BigDecimal(amount), created);
        Instant at = created.plus(daysToWin, ChronoUnit.DAYS);
        o.changeStatus(OpportunityStatus.QUALIFICATION, null, at);
        o.changeStatus(OpportunityStatus.PROPOSAL, null, at);
        o.changeStatus(OpportunityStatus.NEGOTIATION, null, at);
        o.changeStatus(OpportunityStatus.WON, null, at);
        return o;
    }

    private Opportunity lost(User owner, String reason) {
        Opportunity o = new Opportunity(null, null, owner, "Deal", new BigDecimal("1000"),
                NOW.minus(10, ChronoUnit.DAYS));
        o.changeStatus(OpportunityStatus.LOST, reason, NOW.minus(2, ChronoUnit.DAYS));
        return o;
    }

    @Test
    void emptyCompanyHasNoRatesAndNoAverages() {
        DashboardResponse r = DashboardCalculator.calculate(List.of(), List.of(), NOW, 90);

        assertThat(r.conversionRate()).isNull();
        assertThat(r.averageDaysToWin()).isNull();
        assertThat(r.openAmount()).isEqualByComparingTo("0");
        assertThat(r.stages()).hasSize(4);
        assertThat(r.sellers()).isEmpty();
    }

    @Test
    void countsOnlyDealsWithoutActivityForSevenDaysAsStalled() {
        User ana = seller("Ana");
        List<Opportunity> open = List.of(open(ana, "1000", 1), open(ana, "2000", 8), open(ana, "4000", 30));

        DashboardResponse r = DashboardCalculator.calculate(open, List.of(), NOW, 90);

        assertThat(r.openCount()).isEqualTo(3);
        assertThat(r.openAmount()).isEqualByComparingTo("7000");
        assertThat(r.stalledCount()).isEqualTo(2);
        assertThat(r.stalledAmount()).isEqualByComparingTo("6000");
    }

    @Test
    void conversionIsWonOverClosedAndAverageIsDaysToWin() {
        User ana = seller("Ana");
        List<Opportunity> closed = List.of(won(ana, "5000", 10), won(ana, "3000", 20), lost(ana, "Preço"));

        DashboardResponse r = DashboardCalculator.calculate(List.of(), closed, NOW, 90);

        assertThat(r.wonCount()).isEqualTo(2);
        assertThat(r.lostCount()).isEqualTo(1);
        assertThat(r.wonAmount()).isEqualByComparingTo("8000");
        assertThat(r.conversionRate()).isCloseTo(2.0 / 3, within(0.0001));
        assertThat(r.averageDaysToWin()).isEqualTo(15.0);
    }

    @Test
    void lostReasonsAreGroupedIgnoringCaseAndSpacesAndSortedByCount() {
        User ana = seller("Ana");
        List<Opportunity> closed = List.of(lost(ana, "Preço"), lost(ana, " preço "), lost(ana, "Prazo"));

        DashboardResponse r = DashboardCalculator.calculate(List.of(), closed, NOW, 90);

        assertThat(r.lostReasons()).hasSize(2);
        assertThat(r.lostReasons().get(0).reason()).isEqualTo("Preço");
        assertThat(r.lostReasons().get(0).count()).isEqualTo(2);
        assertThat(r.lostReasons().get(1).reason()).isEqualTo("Prazo");
    }

    @Test
    void eachSellerGetsHisOwnNumbersBestSellerFirst() {
        User ana = seller("Ana");
        User bia = seller("Bia");
        List<Opportunity> open = List.of(open(ana, "1000", 20), open(bia, "500", 1));
        List<Opportunity> closed = List.of(won(bia, "9000", 5), lost(ana, "Preço"));

        DashboardResponse r = DashboardCalculator.calculate(open, closed, NOW, 90);

        assertThat(r.sellers()).extracting(DashboardResponse.SellerStats::name).containsExactly("Bia", "Ana");
        DashboardResponse.SellerStats ana1 = r.sellers().get(1);
        assertThat(ana1.stalledCount()).isEqualTo(1);
        assertThat(ana1.lostCount()).isEqualTo(1);
        assertThat(r.sellers().get(0).wonAmount()).isEqualByComparingTo("9000");
    }
}
