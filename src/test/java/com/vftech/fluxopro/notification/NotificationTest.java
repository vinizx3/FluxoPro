package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationTest {

    private static final Instant T0 = Instant.parse("2026-10-23T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-24T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-25T10:00:00Z");

    private Notification newNotification() {
        Company company = new Company("Acme");
        Opportunity opportunity = new Opportunity(company, new Customer(company, "Client", null, null),
                mock(User.class), "Deal", null, T0);
        return new Notification(company, mock(User.class), NotificationType.STALLED_OPPORTUNITY,
                opportunity, T0, T0);
    }

    @Test
    void startsUnread() {
        assertThat(newNotification().isRead()).isFalse();
    }

    @Test
    void markingAsReadStampsTheDate() {
        Notification notification = newNotification();
        notification.markAsRead(T1);
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(T1);
    }

    @Test
    void markingAsReadTwiceKeepsTheFirstDate() {
        Notification notification = newNotification();
        notification.markAsRead(T1);
        notification.markAsRead(T2);
        assertThat(notification.getReadAt()).isEqualTo(T1);
    }
}
