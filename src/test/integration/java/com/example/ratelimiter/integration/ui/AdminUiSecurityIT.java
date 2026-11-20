package com.example.ratelimiter.integration.ui;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.adapter.in.ui.view.ConsumerGroupsView;
import com.example.ratelimiter.adapter.in.ui.view.ConsumersView;
import com.example.ratelimiter.adapter.in.ui.view.PoliciesView;
import com.example.ratelimiter.adapter.in.ui.view.PolicyAssignmentsView;
import com.example.ratelimiter.adapter.in.ui.view.ResourceGroupsView;
import com.example.ratelimiter.adapter.in.ui.view.ResourcesView;
import com.example.ratelimiter.integration.support.Infrastructure;
import com.example.ratelimiter.integration.support.IntegrationTest;
import com.vaadin.flow.router.Route;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** MUC6: the admin UI is protected by form login and covers every configuration table. */
class AdminUiSecurityIT extends IntegrationTest {

    @Test
    void test_muc6_uiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/ui/policies"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void test_muc6_adminCanLogInAndIsSentToTheUi() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user(Infrastructure.ADMIN_USERNAME)
                        .password(Infrastructure.ADMIN_PASSWORD))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/ui/"));
    }

    @Test
    void test_muc6_wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login").user(Infrastructure.ADMIN_USERNAME).password("nope"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void test_muc6_everyConfigurationTableHasAView() {
        List<String> routes = List.of(PoliciesView.class, ConsumerGroupsView.class,
                        ConsumersView.class, ResourceGroupsView.class, ResourcesView.class,
                        PolicyAssignmentsView.class).stream()
                .map(view -> view.getAnnotation(Route.class).value())
                .toList();
        Assertions.assertThat(routes).containsExactly("policies", "consumer-groups", "consumers",
                "resource-groups", "resources", "policy-assignments");
    }
}
