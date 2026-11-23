package com.vftech.fluxopro.opportunity;

import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.common.exception.ConflictException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.opportunity.dto.MoveStatusRequest;
import com.vftech.fluxopro.opportunity.dto.OpportunityRequest;
import com.vftech.fluxopro.opportunity.dto.OpportunityResponse;
import com.vftech.fluxopro.opportunity.dto.OpportunityUpdateRequest;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.opportunity.service.OpportunityService;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.outbox.service.OutboxService;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-16T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-16T12:00:00Z");

    @Mock
    OpportunityRepository opportunities;
    @Mock
    CustomerRepository customers;
    @Mock
    CompanyRepository companies;
    @Mock
    UserRepository users;
    @Mock
    ApplicationEventPublisher events;
    @Mock
    AuditRecorder audit;
    @Mock
    OutboxService outbox;

    private final UUID companyId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private final UUID otherSellerId = UUID.randomUUID();

    private OpportunityService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new OpportunityService(opportunities, customers, companies, users, clock, events, audit, outbox);
    }

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, com.vftech.fluxopro.user.entity.Role.OWNER);
    }

    private AuthenticatedUser seller() {
        return new AuthenticatedUser(sellerId, companyId, com.vftech.fluxopro.user.entity.Role.SELLER);
    }

    private Opportunity opportunityOwnedBy(UUID userId) {
        Company company = new Company("Acme");
        Customer customer = new Customer(company, "Client", null, null);
        User owner = new User(company, "Seller", "seller@acme.com", "hash",
                com.vftech.fluxopro.user.entity.Role.SELLER);
        ReflectionTestUtils.setField(owner, "id", userId);
        Opportunity opportunity = new Opportunity(company, customer, owner, "Deal", BigDecimal.TEN, CREATED_AT);
        ReflectionTestUtils.setField(opportunity, "version", 0L);
        return opportunity;
    }

    @Test
    void ownerBoardShowsAllOpenOpportunitiesOfTheCompany() {
        when(opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
                eq(companyId), anyCollection()))
                .thenReturn(List.of(opportunityOwnedBy(sellerId)));

        Map<OpportunityStatus, List<OpportunityResponse>> board = service.board(owner());

        assertThat(board.keySet()).containsExactly(
                OpportunityStatus.LEAD, OpportunityStatus.QUALIFICATION,
                OpportunityStatus.PROPOSAL, OpportunityStatus.NEGOTIATION);
        assertThat(board.get(OpportunityStatus.LEAD)).hasSize(1);
        assertThat(board.get(OpportunityStatus.PROPOSAL)).isEmpty();
        verify(opportunities, never()).findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
                any(), any(), anyCollection());
    }

    @Test
    void sellerBoardIsRestrictedToTheirOwnOpportunities() {
        when(opportunities.findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
                eq(companyId), eq(sellerId), anyCollection()))
                .thenReturn(List.of(opportunityOwnedBy(sellerId)));

        Map<OpportunityStatus, List<OpportunityResponse>> board = service.board(seller());

        assertThat(board.get(OpportunityStatus.LEAD)).hasSize(1);
        verify(opportunities, never()).findByCompanyIdAndArchivedAtIsNullAndStatusInOrderByLastActivityAtAsc(
                any(), anyCollection());
    }

    @Test
    void sellerCannotSeeAnotherSellersOpportunity() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(otherSellerId)));

        assertThatThrownBy(() -> service.get(seller(), id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Opportunity not found");
    }

    @Test
    void ownerCanSeeAnySellersOpportunity() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(otherSellerId)));

        OpportunityResponse response = service.get(owner(), id);

        assertThat(response.status()).isEqualTo(OpportunityStatus.LEAD);
    }

    @Test
    void opportunityFromAnotherCompanyIsNotFound() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(owner(), id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void moveStampsTheStatusDateWithTheClock() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(sellerId)));

        OpportunityResponse response = service.move(
                seller(), id, new MoveStatusRequest(OpportunityStatus.QUALIFICATION, null, 0L));

        assertThat(response.status()).isEqualTo(OpportunityStatus.QUALIFICATION);
        assertThat(response.statusChangedAt()).isEqualTo(NOW);
    }

    @Test
    void invalidMoveIsRejectedByBusinessRule() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(sellerId)));

        assertThatThrownBy(() -> service.move(
                seller(), id, new MoveStatusRequest(OpportunityStatus.WON, null, 0L)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createRejectsCustomerFromAnotherCompany() {
        UUID customerId = UUID.randomUUID();
        when(customers.findByIdAndCompanyIdAndArchivedAtIsNull(customerId, companyId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                owner(), new OpportunityRequest(customerId, "Deal", BigDecimal.TEN)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Customer not found");

        verify(opportunities, never()).save(any());
    }

    @Test
    void moveWithStaleVersionIsRejectedAndKeepsStatus() {
        UUID id = UUID.randomUUID();
        Opportunity opportunity = opportunityOwnedBy(sellerId);
        when(opportunities.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.move(
                seller(), id, new MoveStatusRequest(OpportunityStatus.QUALIFICATION, null, 5L)))
                .isInstanceOf(ConflictException.class);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LEAD);
        verify(opportunities, never()).saveAndFlush(any());
    }

    @Test
    void archivingAnOpenOpportunityIsRejected() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(sellerId)));

        assertThatThrownBy(() -> service.archive(seller(), id))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sellerCannotEditAnotherSellersOpportunity() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(otherSellerId)));

        assertThatThrownBy(() -> service.update(
                seller(), id, new OpportunityUpdateRequest("New title", BigDecimal.ONE, 0L)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void moveToWonRecordsTheStatusChangeInTheOutbox() {
        UUID id = UUID.randomUUID();
        Opportunity opportunity = opportunityOwnedBy(sellerId);
        ReflectionTestUtils.setField(opportunity, "id", id);
        ReflectionTestUtils.setField(opportunity.getCompany(), "id", companyId);
        opportunity.changeStatus(OpportunityStatus.QUALIFICATION, null, CREATED_AT);
        opportunity.changeStatus(OpportunityStatus.PROPOSAL, null, CREATED_AT);
        opportunity.changeStatus(OpportunityStatus.NEGOTIATION, null, CREATED_AT);
        when(opportunities.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(opportunity));

        service.move(seller(), id, new MoveStatusRequest(OpportunityStatus.WON, null, 0L));

        ArgumentCaptor<OpportunityEventPayload> payload = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(outbox).append(eq("OPPORTUNITY"), eq(id), eq("OPPORTUNITY_STATUS_CHANGED"),
                eq(companyId), payload.capture());
        assertThat(payload.getValue().fromStatus()).isEqualTo("NEGOTIATION");
        assertThat(payload.getValue().toStatus()).isEqualTo("WON");
        assertThat(payload.getValue().ownerId()).isEqualTo(sellerId);
        assertThat(payload.getValue().actorId()).isEqualTo(sellerId);
        assertThat(payload.getValue().title()).isEqualTo("Deal");
        assertThat(payload.getValue().amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(payload.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void createRecordsTheCreationInTheOutbox() {
        AuthenticatedUser user = owner();
        UUID customerId = UUID.randomUUID();
        Company company = new Company("Acme");
        Customer customer = new Customer(company, "Client", null, null);
        User creator = new User(company, "Owner", "owner@acme.com", "hash",
                com.vftech.fluxopro.user.entity.Role.OWNER);
        when(customers.findByIdAndCompanyIdAndArchivedAtIsNull(customerId, companyId))
                .thenReturn(Optional.of(customer));
        when(companies.getReferenceById(companyId)).thenReturn(company);
        when(users.getReferenceById(user.id())).thenReturn(creator);
        when(opportunities.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(user, new OpportunityRequest(customerId, "  Deal  ", BigDecimal.TEN));

        ArgumentCaptor<OpportunityEventPayload> payload = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(outbox).append(eq("OPPORTUNITY"), any(), eq("OPPORTUNITY_CREATED"),
                eq(companyId), payload.capture());
        assertThat(payload.getValue().fromStatus()).isNull();
        assertThat(payload.getValue().toStatus()).isEqualTo("LEAD");
        assertThat(payload.getValue().title()).isEqualTo("Deal");
        assertThat(payload.getValue().actorId()).isEqualTo(user.id());
        assertThat(payload.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void invalidMoveDoesNotWriteToTheOutbox() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(sellerId)));

        assertThatThrownBy(() -> service.move(
                seller(), id, new MoveStatusRequest(OpportunityStatus.WON, null, 0L)))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(outbox);
    }

    @Test
    void moveWithStaleVersionDoesNotWriteToTheOutbox() {
        UUID id = UUID.randomUUID();
        when(opportunities.findByIdAndCompanyId(id, companyId))
                .thenReturn(Optional.of(opportunityOwnedBy(sellerId)));

        assertThatThrownBy(() -> service.move(
                seller(), id, new MoveStatusRequest(OpportunityStatus.QUALIFICATION, null, 5L)))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(outbox);
    }
}
