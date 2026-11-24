package com.vftech.fluxopro.funnel;

import com.vftech.fluxopro.funnel.service.FunnelProjection;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FunnelProjectionIntegrationTest {

    @Autowired
    FunnelProjection projection;

    @Autowired
    JdbcClient jdbc;

    @Test
    void firstEventCreatesTheStageRow() {
        UUID company = newCompany();

        boolean applied = projection.apply(UUID.randomUUID(), payload(company, "PROPOSAL", "1500.00"));

        assertThat(applied).isTrue();
        assertThat(entries(company, "PROPOSAL")).isEqualTo(1L);
        assertThat(amount(company, "PROPOSAL")).isEqualByComparingTo("1500.00");
    }

    @Test
    void sameEventDeliveredTwiceIsCountedOnce() {
        UUID company = newCompany();
        UUID eventId = UUID.randomUUID();

        boolean first = projection.apply(eventId, payload(company, "NEGOTIATION", "800.00"));
        boolean second = projection.apply(eventId, payload(company, "NEGOTIATION", "800.00"));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(entries(company, "NEGOTIATION")).isEqualTo(1L);
        assertThat(amount(company, "NEGOTIATION")).isEqualByComparingTo("800.00");
    }

    @Test
    void differentEventsOnTheSameStageAccumulate() {
        UUID company = newCompany();

        projection.apply(UUID.randomUUID(), payload(company, "WON", "1000.00"));
        projection.apply(UUID.randomUUID(), payload(company, "WON", "250.50"));

        assertThat(entries(company, "WON")).isEqualTo(2L);
        assertThat(amount(company, "WON")).isEqualByComparingTo("1250.50");
    }

    @Test
    void nullAmountCountsAsZero() {
        UUID company = newCompany();

        projection.apply(UUID.randomUUID(), payload(company, "LEAD", null));

        assertThat(entries(company, "LEAD")).isEqualTo(1L);
        assertThat(amount(company, "LEAD")).isEqualByComparingTo("0");
    }

    @Test
    void companiesAreIsolatedFromEachOther() {
        UUID companyA = newCompany();
        UUID companyB = newCompany();

        projection.apply(UUID.randomUUID(), payload(companyA, "QUALIFICATION", "100.00"));
        projection.apply(UUID.randomUUID(), payload(companyB, "QUALIFICATION", "300.00"));

        assertThat(entries(companyA, "QUALIFICATION")).isEqualTo(1L);
        assertThat(amount(companyA, "QUALIFICATION")).isEqualByComparingTo("100.00");
        assertThat(entries(companyB, "QUALIFICATION")).isEqualTo(1L);
        assertThat(amount(companyB, "QUALIFICATION")).isEqualByComparingTo("300.00");
    }

    private UUID newCompany() {
        return jdbc.sql("INSERT INTO company (name) VALUES (:name) RETURNING id")
                .param("name", "Empresa de teste " + UUID.randomUUID())
                .query(UUID.class)
                .single();
    }

    private OpportunityEventPayload payload(UUID company, String toStatus, String amount) {
        return new OpportunityEventPayload(
                UUID.randomUUID(), company, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Contrato de teste", amount == null ? null : new BigDecimal(amount),
                "NEGOTIATION", toStatus, Instant.now());
    }

    private long entries(UUID company, String stage) {
        return jdbc.sql("SELECT entries FROM funnel_stage_entry WHERE company_id = :c AND stage = :s")
                .param("c", company).param("s", stage)
                .query(Long.class).single();
    }

    private BigDecimal amount(UUID company, String stage) {
        return jdbc.sql("SELECT entered_amount FROM funnel_stage_entry WHERE company_id = :c AND stage = :s")
                .param("c", company).param("s", stage)
                .query(BigDecimal.class).single();
    }
}
