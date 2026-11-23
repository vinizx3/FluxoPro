package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.notification.entity.SentEmail;
import com.vftech.fluxopro.notification.repository.SentEmailRepository;
import com.vftech.fluxopro.notification.service.ClosedOpportunityMailer;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClosedOpportunityMailerTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private JavaMailSender mail;
    @Mock
    private UserRepository users;
    @Mock
    private SentEmailRepository sentEmails;

    private final UUID companyId = UUID.randomUUID();
    private final UUID companyOwnerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final Company company = new Company("Acme");

    private ClosedOpportunityMailer mailer;

    @BeforeEach
    void setUp() {
        mailer = new ClosedOpportunityMailer(mail, users, sentEmails,
                Clock.fixed(NOW, ZoneOffset.UTC), "no-reply@fluxopro.local");
    }

    private User user(UUID id, String email, Role role) {
        User user = new User(company, "Name", email, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private OpportunityEventPayload payload(String toStatus) {
        return new OpportunityEventPayload(UUID.randomUUID(), companyId, UUID.randomUUID(), sellerId, sellerId,
                "Deal", new BigDecimal("10000.00"), "NEGOTIATION", toStatus, NOW);
    }

    @Test
    void sendsOneEmailPerRecipientAndRecordsEachOne() {
        User companyOwner = user(companyOwnerId, "owner@acme.com", Role.OWNER);
        User seller = user(sellerId, "seller@acme.com", Role.SELLER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(users.findByIdAndCompanyId(sellerId, companyId)).thenReturn(Optional.of(seller));

        int sent = mailer.sendClosed(eventId, payload("WON"));

        assertThat(sent).isEqualTo(2);
        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(2)).send(messages.capture());
        assertThat(messages.getAllValues().stream().map(m -> m.getTo()[0]).toList())
                .containsExactlyInAnyOrder("owner@acme.com", "seller@acme.com");
        assertThat(messages.getAllValues()).allSatisfy(message -> {
            assertThat(message.getFrom()).isEqualTo("no-reply@fluxopro.local");
            assertThat(message.getSubject()).isEqualTo("Negociação ganha: Deal");
            assertThat(message.getText()).contains("10.000,00");
        });
        verify(sentEmails, times(2)).save(any(SentEmail.class));
    }

    @Test
    void usesTheLostSubjectWhenTheOpportunityWasLost() {
        User companyOwner = user(companyOwnerId, "owner@acme.com", Role.OWNER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(users.findByIdAndCompanyId(sellerId, companyId)).thenReturn(Optional.empty());

        mailer.sendClosed(eventId, payload("LOST"));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertThat(message.getValue().getSubject()).isEqualTo("Negociação perdida: Deal");
    }

    @Test
    void doesNotEmailAnInactiveOpportunityOwner() {
        User companyOwner = user(companyOwnerId, "owner@acme.com", Role.OWNER);
        User inactiveSeller = user(sellerId, "seller@acme.com", Role.SELLER);
        inactiveSeller.setActive(false);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(users.findByIdAndCompanyId(sellerId, companyId)).thenReturn(Optional.of(inactiveSeller));

        int sent = mailer.sendClosed(eventId, payload("WON"));

        assertThat(sent).isEqualTo(1);
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void doesNotResendToRecipientsAlreadyEmailed() {
        User companyOwner = user(companyOwnerId, "owner@acme.com", Role.OWNER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(users.findByIdAndCompanyId(sellerId, companyId)).thenReturn(Optional.empty());
        when(sentEmails.existsByEventIdAndUserId(eventId, companyOwnerId)).thenReturn(true);

        int sent = mailer.sendClosed(eventId, payload("WON"));

        assertThat(sent).isZero();
        verifyNoInteractions(mail);
        verify(sentEmails, never()).save(any(SentEmail.class));
    }

    @Test
    void doesNotRecordTheEmailWhenSendingFails() {
        User companyOwner = user(companyOwnerId, "owner@acme.com", Role.OWNER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(users.findByIdAndCompanyId(sellerId, companyId)).thenReturn(Optional.empty());
        doThrow(new MailSendException("smtp down")).when(mail).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> mailer.sendClosed(eventId, payload("WON")))
                .isInstanceOf(MailSendException.class);

        verify(sentEmails, never()).save(any(SentEmail.class));
    }
}