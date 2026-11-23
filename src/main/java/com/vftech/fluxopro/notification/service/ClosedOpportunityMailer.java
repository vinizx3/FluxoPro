package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.notification.entity.SentEmail;
import com.vftech.fluxopro.notification.repository.SentEmailRepository;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ClosedOpportunityMailer {

    private final JavaMailSender mail;
    private final UserRepository users;
    private final SentEmailRepository sentEmails;
    private final Clock clock;
    private final String from;

    public ClosedOpportunityMailer(JavaMailSender mail, UserRepository users, SentEmailRepository sentEmails, Clock clock, @Value("${app.mail.from}") String from) {
        this.mail = mail;
        this.users = users;
        this.sentEmails = sentEmails;
        this.clock = clock;
        this.from = from;
    }

    public int sendClosed(UUID eventId, OpportunityEventPayload payload) {
        Map<UUID, User> recipients = new LinkedHashMap<>();
        for (User owner : users.findByCompanyIdAndRoleAndActiveTrue(payload.companyId(), Role.OWNER)) {
            recipients.put(owner.getId(), owner);
        }
        users.findByIdAndCompanyId(payload.ownerId(), payload.companyId())
                .filter(User::isActive)
                .ifPresent(owner -> recipients.putIfAbsent(owner.getId(), owner));

        boolean won = "WON".equals(payload.toStatus());
        int sent = 0;
        for (User recipient : recipients.values()) {
            if (sentEmails.existsByEventIdAndUserId(eventId, recipient.getId())) {
                continue;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient.getEmail());
            message.setSubject((won ? "Negociação ganha: " : "Negociação perdida: ") + payload.title());
            message.setText(body(recipient, payload, won));
            mail.send(message);
            sentEmails.save(new SentEmail(eventId, recipient.getId(), Instant.now(clock)));
            sent++;
        }
        return sent;
    }

    private static String body(User recipient, OpportunityEventPayload payload, boolean won) {
        return "Olá, " + recipient.getName() + "!\n\n"
                + "A negociação \"" + payload.title() + "\" foi marcada como "
                + (won ? "GANHA" : "PERDIDA") + ".\n"
                + "Valor: " + money(payload.amount()) + "\n\n"
                + "Acesse o FluxoPro para ver os detalhes.";
    }

    private static String money(BigDecimal amount) {
        return amount == null
                ? "não informado"
                : NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(amount);
    }
}
