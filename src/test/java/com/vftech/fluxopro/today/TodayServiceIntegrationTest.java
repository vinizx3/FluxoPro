package com.vftech.fluxopro.today;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.support.IntegrationTest;
import com.vftech.fluxopro.today.dto.TodayResponse;
import com.vftech.fluxopro.today.service.TodayService;
import com.vftech.fluxopro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class TodayServiceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    TodayService todayService;

    @Autowired
    JdbcClient jdbc;

    @Test
    void groupsOverdueDueTodayAndWithoutNextStep() {
        UUID company = newCompany();
        UUID owner = newUser(company, "OWNER");
        UUID customer = newCustomer(company);

        newOpportunity(company, customer, owner, "Atrasada", "LEAD", TODAY.minusDays(3));
        newOpportunity(company, customer, owner, "De hoje", "PROPOSAL", TODAY);
        newOpportunity(company, customer, owner, "Futura", "NEGOTIATION", TODAY.plusDays(2));
        newOpportunity(company, customer, owner, "Sem passo", "QUALIFICATION", null);
        newOpportunity(company, customer, owner, "Ja ganha", "WON", null);

        TodayResponse today = todayService.build(new AuthenticatedUser(owner, company, Role.OWNER));

        assertThat(today.date()).isEqualTo(TODAY);
        assertThat(titles(today.overdue())).containsExactly("Atrasada");
        assertThat(today.overdue().get(0).daysOverdue()).isEqualTo(3);
        assertThat(titles(today.dueToday())).containsExactly("De hoje");
        assertThat(titles(today.withoutNextStep())).containsExactly("Sem passo");
    }

    @Test
    void sellerSeesOnlyOwnOpportunitiesButOwnerSeesAll() {
        UUID company = newCompany();
        UUID owner = newUser(company, "OWNER");
        UUID seller = newUser(company, "SELLER");
        UUID customer = newCustomer(company);

        newOpportunity(company, customer, owner, "Do dono", "LEAD", TODAY.minusDays(1));
        newOpportunity(company, customer, seller, "Do vendedor", "LEAD", TODAY.minusDays(1));

        TodayResponse asSeller = todayService.build(new AuthenticatedUser(seller, company, Role.SELLER));
        TodayResponse asOwner = todayService.build(new AuthenticatedUser(owner, company, Role.OWNER));

        assertThat(titles(asSeller.overdue())).containsExactly("Do vendedor");
        assertThat(titles(asOwner.overdue())).containsExactlyInAnyOrder("Do dono", "Do vendedor");
    }

    @Test
    void neverShowsAnotherCompanysOpportunities() {
        UUID mine = newCompany();
        UUID myOwner = newUser(mine, "OWNER");
        UUID other = newCompany();
        UUID otherOwner = newUser(other, "OWNER");
        UUID otherCustomer = newCustomer(other);

        newOpportunity(other, otherCustomer, otherOwner, "De outra empresa", "LEAD", TODAY.minusDays(5));

        TodayResponse today = todayService.build(new AuthenticatedUser(myOwner, mine, Role.OWNER));

        assertThat(today.overdue()).isEmpty();
        assertThat(today.dueToday()).isEmpty();
        assertThat(today.withoutNextStep()).isEmpty();
    }

    private static List<String> titles(List<TodayResponse.Item> items) {
        return items.stream().map(TodayResponse.Item::title).toList();
    }

    private UUID newCompany() {
        return jdbc.sql("INSERT INTO company (name) VALUES (:name) RETURNING id")
                .param("name", "Empresa " + UUID.randomUUID())
                .query(UUID.class).single();
    }

    private UUID newUser(UUID company, String role) {
        return jdbc.sql("""
                        INSERT INTO users (company_id, name, email, password_hash, role)
                        VALUES (:company, :name, :email, 'not-a-real-hash', :role) RETURNING id
                        """)
                .param("company", company)
                .param("name", role + " de teste")
                .param("email", role.toLowerCase() + "-" + UUID.randomUUID() + "@teste.local")
                .param("role", role)
                .query(UUID.class).single();
    }

    private UUID newCustomer(UUID company) {
        return jdbc.sql("INSERT INTO customer (company_id, name) VALUES (:company, 'Cliente de teste') RETURNING id")
                .param("company", company)
                .query(UUID.class).single();
    }

    private void newOpportunity(UUID company, UUID customer, UUID owner,
                                String title, String status, LocalDate nextStepAt) {
        jdbc.sql("""
                        INSERT INTO opportunity (company_id, customer_id, owner_id, title, status,
                                                 last_activity_at, next_step_at)
                        VALUES (:company, :customer, :owner, :title, :status, now(), :next)
                        """)
                .param("company", company)
                .param("customer", customer)
                .param("owner", owner)
                .param("title", title)
                .param("status", status)
                .param("next", nextStepAt, Types.DATE)
                .update();
    }
}
