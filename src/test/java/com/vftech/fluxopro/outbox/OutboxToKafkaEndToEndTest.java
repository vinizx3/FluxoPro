package com.vftech.fluxopro.outbox;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.opportunity.dto.MoveStatusRequest;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.service.OpportunityService;
import com.vftech.fluxopro.support.IntegrationTest;
import com.vftech.fluxopro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OutboxToKafkaEndToEndTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    OpportunityService opportunityService;

    @Autowired
    JdbcClient jdbc;

    @Test
    void wonOpportunityFlowsThroughOutboxAndKafkaToNotificationAndFunnel() throws Exception {
        Scenario s = scenario("NEGOTIATION");

        opportunityService.move(s.owner(), s.opportunityId(),
                new MoveStatusRequest(OpportunityStatus.WON, null, 0L));

        awaitTrue("a WON notification for the owner",
                () -> notifications(s.opportunityId(), "OPPORTUNITY_WON") == 1);

        assertThat(outboxEvents(s.opportunityId(), "OPPORTUNITY_STATUS_CHANGED")).isEqualTo(1);
        assertThat(unpublishedOutboxEvents(s.opportunityId())).isZero();

        awaitTrue("the funnel entry for WON", () -> funnelEntries(s.companyId(), "WON") == 1);
        assertThat(funnelEntries(s.companyId(), "WON")).isEqualTo(1L);
    }

    @Test
    void lostOpportunityCreatesALostNotification() throws Exception {
        Scenario s = scenario("LEAD");

        opportunityService.move(s.owner(), s.opportunityId(),
                new MoveStatusRequest(OpportunityStatus.LOST, "Preço acima do orçamento", 0L));

        awaitTrue("a LOST notification for the owner",
                () -> notifications(s.opportunityId(), "OPPORTUNITY_LOST") == 1);

        assertThat(notifications(s.opportunityId(), "OPPORTUNITY_WON")).isZero();
        assertThat(unpublishedOutboxEvents(s.opportunityId())).isZero();
    }

    private Scenario scenario(String initialStatus) {
        UUID companyId = jdbc.sql("INSERT INTO company (name) VALUES (:name) RETURNING id")
                .param("name", "Empresa " + UUID.randomUUID())
                .query(UUID.class).single();
        UUID ownerId = jdbc.sql("""
                        INSERT INTO users (company_id, name, email, password_hash, role)
                        VALUES (:company, 'Dono de teste', :email, 'not-a-real-hash', 'OWNER') RETURNING id
                        """)
                .param("company", companyId)
                .param("email", "dono-" + UUID.randomUUID() + "@teste.local")
                .query(UUID.class).single();
        UUID customerId = jdbc.sql("INSERT INTO customer (company_id, name) VALUES (:company, 'Cliente de teste') RETURNING id")
                .param("company", companyId)
                .query(UUID.class).single();
        UUID opportunityId = jdbc.sql("""
                        INSERT INTO opportunity (company_id, customer_id, owner_id, title, amount, status, last_activity_at)
                        VALUES (:company, :customer, :owner, 'Contrato de teste', :amount, :status, now()) RETURNING id
                        """)
                .param("company", companyId)
                .param("customer", customerId)
                .param("owner", ownerId)
                .param("amount", new BigDecimal("10000.00"))
                .param("status", initialStatus)
                .query(UUID.class).single();
        return new Scenario(companyId, opportunityId, new AuthenticatedUser(ownerId, companyId, Role.OWNER));
    }

    private long notifications(UUID opportunityId, String type) {
        return jdbc.sql("SELECT count(*) FROM notification WHERE opportunity_id = :id AND type = :type")
                .param("id", opportunityId).param("type", type)
                .query(Long.class).single();
    }

    private long outboxEvents(UUID opportunityId, String eventType) {
        return jdbc.sql("SELECT count(*) FROM outbox_event WHERE aggregate_id = :id AND event_type = :type")
                .param("id", opportunityId).param("type", eventType)
                .query(Long.class).single();
    }

    private long unpublishedOutboxEvents(UUID opportunityId) {
        return jdbc.sql("SELECT count(*) FROM outbox_event WHERE aggregate_id = :id AND published_at IS NULL")
                .param("id", opportunityId)
                .query(Long.class).single();
    }

    private long funnelEntries(UUID companyId, String stage) {
        return jdbc.sql("SELECT entries FROM funnel_stage_entry WHERE company_id = :c AND stage = :s")
                .param("c", companyId).param("s", stage)
                .query(Long.class).optional().orElse(0L);
    }

    private void awaitTrue(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Timed out waiting for: " + description);
    }

    private record Scenario(UUID companyId, UUID opportunityId, AuthenticatedUser owner) {
    }
}
