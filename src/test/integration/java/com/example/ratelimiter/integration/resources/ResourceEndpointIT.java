package com.example.ratelimiter.integration.resources;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** MUC1.4: /admin/api/resources, including the OpenAPI synchronisation endpoint. */
@Transactional
class ResourceEndpointIT extends IntegrationTest {

    private static final String RESOURCES = "/admin/api/resources";

    @Test
    void test_muc1_fetchesAllEndpointsFromTargetOpenApi() throws Exception {
        adminPost(RESOURCES + "/sync", Map.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(15))
                .andExpect(jsonPath("$.unchanged").value(0));

        adminGet(RESOURCES + "?method=GET&size=50").andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(6))
                .andExpect(jsonPath("$.content[*].io_type", hasItems("read")));
        adminGet(RESOURCES + "?io_type=write&size=50")
                .andExpect(jsonPath("$.total_elements").value(9));
        adminGet("/admin/api/resource-groups").andExpect(jsonPath("$.content[*].name",
                hasItems("orders", "users", "carts")));
    }

    @Test
    void test_muc1_syncIsIdempotentAndKeepsManualChanges() throws Exception {
        adminPost(RESOURCES + "/sync", Map.of()).andExpect(status().isOk());
        long id = resourceId("GET", "/orders");
        adminPut(RESOURCES + "/" + id, ioTypeAndGroup("write", null)).andExpect(status().isOk());

        adminPost(RESOURCES + "/sync", Map.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(0))
                .andExpect(jsonPath("$.unchanged").value(15));
        adminGet(RESOURCES + "/{id}", id).andExpect(jsonPath("$.io_type").value("write"))
                .andExpect(jsonPath("$.resource_group_id").isEmpty());
    }

    @Test
    void test_muc1_registersResourceManuallyWithDefaultIoType() throws Exception {
        long id = createAndGetId(RESOURCES, Map.of("method", "PUT", "resource_path",
                "reports/{id}"));
        adminGet(RESOURCES + "/{id}", id)
                .andExpect(jsonPath("$.resource_path").value("/reports/{id}"))
                .andExpect(jsonPath("$.io_type").value("write"));
        adminPost(RESOURCES, Map.of("method", "PUT", "resource_path", "/reports/{id}"))
                .andExpect(status().isConflict());
        adminPost(RESOURCES, Map.of("method", "FETCH", "resource_path", "/x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test_muc1_assignedResourceCannotBeDeleted() throws Exception {
        adminPost(RESOURCES + "/sync", Map.of()).andExpect(status().isOk());
        long resource = resourceId("GET", "/orders");
        long group = createAndGetId("/admin/api/consumer-groups",
                Map.of("group_name", "free", "consumer_ids", List.of("f-1")));
        long policy = createAndGetId("/admin/api/policies",
                Map.of("name", "p", "number_of_requests", 1, "time_period", "minute"));
        createAndGetId("/admin/api/consumer-groups-policies", Map.of("consumer_group_id", group,
                "relationship_type", "resource", "policy_id", policy, "resource_id", resource));

        adminDelete(RESOURCES + "/" + resource).andExpect(status().isConflict());
        adminDelete(RESOURCES + "/" + resourceId("GET", "/users"))
                .andExpect(status().isNoContent());
    }

    private static Map<String, Object> ioTypeAndGroup(String ioType, Long groupId) {
        Map<String, Object> body = new HashMap<>();
        body.put("io_type", ioType);
        body.put("resource_group_id", groupId);
        return body;
    }
}
