package com.example.ratelimiter.integration.consumergroups;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** MUC1.1: /admin/api/consumer-groups (starts from an empty configuration). */
@Transactional
class ConsumerGroupEndpointIT extends IntegrationTest {

    private static final String GROUPS = "/admin/api/consumer-groups";

    @Test
    void test_muc1_createsConsumerGroupWithItsFirstConsumers() throws Exception {
        long id = createAndGetId(GROUPS,
                Map.of("group_name", "premium", "consumer_ids", List.of("p-1", "p-2")));

        adminGet(GROUPS + "/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.group_name").value("premium"));
        adminGet("/admin/api/consumers?consumer_group_id=" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(2));
    }

    @Test
    void test_muc1_consumerGroupCannotBeEmpty() throws Exception {
        adminPost(GROUPS, Map.of("group_name", "free", "consumer_ids", List.of()))
                .andExpect(status().isBadRequest());
        adminPost(GROUPS, Map.of("group_name", "free")).andExpect(status().isBadRequest());
    }

    @Test
    void test_muc1_groupNameIsAnEnumAndUnique() throws Exception {
        adminPost(GROUPS, Map.of("group_name", "gold", "consumer_ids", List.of("x")))
                .andExpect(status().isBadRequest());
        createAndGetId(GROUPS, Map.of("group_name", "free", "consumer_ids", List.of("f-1")));
        adminPost(GROUPS, Map.of("group_name", "free", "consumer_ids", List.of("f-2")))
                .andExpect(status().isConflict());
        adminGet(GROUPS + "/names").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItems("premium", "regular", "free", "internal")));
    }

    @Test
    void test_muc1_consumerIdsAreGloballyUnique() throws Exception {
        createAndGetId(GROUPS, Map.of("group_name", "free", "consumer_ids", List.of("alice")));
        adminPost(GROUPS, Map.of("group_name", "premium", "consumer_ids", List.of("alice")))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc1_renamesAndDeletesGroupWithItsConsumers() throws Exception {
        long id = createAndGetId(GROUPS,
                Map.of("group_name", "regular", "consumer_ids", List.of("r-1")));

        adminPut(GROUPS + "/" + id, Map.of("group_name", "internal")).andExpect(status().isOk())
                .andExpect(jsonPath("$.group_name").value("internal"));
        adminDelete(GROUPS + "/" + id).andExpect(status().isNoContent());
        adminGet(GROUPS + "/{id}", id).andExpect(status().isNotFound());
        adminGet("/admin/api/consumers?consumer_id=r-1")
                .andExpect(jsonPath("$.total_elements").value(0));
    }
}
