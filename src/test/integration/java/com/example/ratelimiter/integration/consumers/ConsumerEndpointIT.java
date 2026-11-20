package com.example.ratelimiter.integration.consumers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** MUC1.2: /admin/api/consumers. */
@Transactional
@Sql(IntegrationTest.SEED)
class ConsumerEndpointIT extends IntegrationTest {

    private static final String CONSUMERS = "/admin/api/consumers";

    @Test
    void test_muc1_createsConsumerAndAssignsConsumerGroup() throws Exception {
        long groupId = consumerGroupId("premium");
        long id = createAndGetId(CONSUMERS,
                Map.of("consumer_id", "premium-user-3", "consumer_group_id", groupId));

        adminGet(CONSUMERS + "/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.consumer_id").value("premium-user-3"))
                .andExpect(jsonPath("$.consumer_group_id").value(groupId));
    }

    @Test
    void test_muc1_consumerAlwaysBelongsToAnExistingGroup() throws Exception {
        adminPost(CONSUMERS, Map.of("consumer_id", "orphan")).andExpect(status().isBadRequest());
        adminPost(CONSUMERS, Map.of("consumer_id", "orphan", "consumer_group_id", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void test_muc1_rejectsDuplicateConsumerId() throws Exception {
        adminPost(CONSUMERS, Map.of("consumer_id", "free-user-1",
                "consumer_group_id", consumerGroupId("premium")))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc1_lastConsumerOfAGroupCannotBeRemoved() throws Exception {
        long id = idOf("select id from consumers where consumer_id = 'internal-service-1'");
        adminDelete(CONSUMERS + "/" + id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("at least one consumer")));
        adminPut(CONSUMERS + "/" + id, Map.of("consumer_id", "internal-service-1",
                "consumer_group_id", consumerGroupId("free")))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc1_movesConsumerToAnotherGroupAndDeletesIt() throws Exception {
        long id = idOf("select id from consumers where consumer_id = 'regular-user-2'");
        long premium = consumerGroupId("premium");

        adminPut(CONSUMERS + "/" + id, Map.of("consumer_id", "regular-user-2",
                "consumer_group_id", premium))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumer_group_id").value(premium));
        adminDelete(CONSUMERS + "/" + id).andExpect(status().isNoContent());
    }

    @Test
    void test_muc1_filtersAndPaginatesConsumers() throws Exception {
        adminGet(CONSUMERS + "?consumer_group_id=" + consumerGroupId("free") + "&size=1&page=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1));
        adminGet(CONSUMERS + "?consumer_id=premium")
                .andExpect(jsonPath("$.total_elements").value(2));
    }
}
