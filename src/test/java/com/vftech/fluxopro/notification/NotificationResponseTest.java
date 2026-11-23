package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.notification.dto.NotificationResponse;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    private final Company company = new Company("Acme");
    private final User seller = new User(company, "Seller", "seller@acme.com", "hash", Role.SELLER);
    private final Opportunity opportunity = new Opportunity(company,
            new Customer(company, "Hotel Planalto", null, null), seller, "Deal", BigDecimal.TEN, NOW);

    @Test
    void closedOpportunityNotificationHasNoStalledDays() {
        Notification notification = Notification.forClosedOpportunity(
                company, seller, NotificationType.OPPORTUNITY_WON, opportunity, UUID.randomUUID(), NOW);

        NotificationResponse response = NotificationResponse.from(notification, NOW.plus(Duration.ofDays(3)));

        assertThat(response.type()).isEqualTo(NotificationType.OPPORTUNITY_WON);
        assertThat(response.stalledSince()).isNull();
        assertThat(response.daysStalled()).isZero();
        assertThat(response.customerName()).isEqualTo("Hotel Planalto");
    }

    @Test
    void stalledNotificationReportsTheDaysSinceItStalled() {
        Notification notification = new Notification(company, seller, NotificationType.STALLED_OPPORTUNITY,
                opportunity, NOW.minus(Duration.ofDays(5)), NOW);

        NotificationResponse response = NotificationResponse.from(notification, NOW);

        assertThat(response.daysStalled()).isEqualTo(5);
    }
}
