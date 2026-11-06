package com.vftech.fluxopro.contact.service;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.contact.dto.ContactResponse;
import com.vftech.fluxopro.contact.dto.RegisterContactRequest;
import com.vftech.fluxopro.contact.entity.OpportunityContact;
import com.vftech.fluxopro.contact.repository.OpportunityContactRepository;
import com.vftech.fluxopro.opportunity.dto.OpportunityResponse;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class OpportunityContactService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final OpportunityRepository opportunities;
    private final OpportunityContactRepository contacts;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final Clock clock;

    public OpportunityContactService(OpportunityRepository opportunities,
                                     OpportunityContactRepository contacts,
                                     CompanyRepository companies,
                                     UserRepository users,
                                     Clock clock) {
        this.opportunities = opportunities;
        this.contacts = contacts;
        this.companies = companies;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public OpportunityResponse register(AuthenticatedUser user, UUID opportunityId,
                                        RegisterContactRequest request) {
        Opportunity opportunity = findVisible(user, opportunityId);
        Instant now = Instant.now(clock);

        LocalDate nextStepAt = request.nextStepAt();
        if (nextStepAt != null && nextStepAt.isBefore(LocalDate.ofInstant(now, ZONE))) {
            throw new BusinessRuleException("The next step cannot be in the past");
        }

        String nextStepNote = nextStepAt == null ? null : blankToNull(request.nextStepNote());

        opportunity.registerContact(nextStepAt, nextStepNote, now);
        contacts.save(new OpportunityContact(
                companies.getReferenceById(user.companyId()),
                opportunity,
                users.getReferenceById(user.id()),
                request.kind(),
                request.note().trim(),
                now));
        opportunities.saveAndFlush(opportunity);
        return OpportunityResponse.from(opportunity);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> list(AuthenticatedUser user, UUID opportunityId) {
        Opportunity opportunity = findVisible(user, opportunityId);
        return contacts
                .findTop100ByOpportunityIdAndCompanyIdOrderByContactedAtDesc(
                        opportunity.getId(), user.companyId())
                .stream()
                .map(ContactResponse::from)
                .toList();
    }

    private Opportunity findVisible(AuthenticatedUser user, UUID id) {
        return opportunities.findByIdAndCompanyId(id, user.companyId())
                .filter(o -> user.isOwner() || o.getOwner().getId().equals(user.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
