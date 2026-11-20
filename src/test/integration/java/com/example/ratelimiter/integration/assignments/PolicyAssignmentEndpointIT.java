package com.example.ratelimiter.integration.assignments;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * MUC1.5 / MUC2: /admin/api/consumer-groups-policies. Seed: premium and regular have one
 * assignment per resource group, free has resource-level assignments for all writes and for
 * the reads of /orders.
 */
@Transactional
@Sql(IntegrationTest.SEED)
class PolicyAssignmentEndpointIT extends IntegrationTest {

    private static final String ASSIGNMENTS = "/admin/api/consumer-groups-policies";

    @Test
    void test_muc1_assignsPolicyToASingleResource() throws Exception {
        long id = createAndGetId(ASSIGNMENTS, assignment("free", "resource",
                "free-10-per-minute", resourceId("GET", "/users/{id}")));

        adminGet(ASSIGNMENTS + "/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.relationship_type").value("resource"))
                .andExpect(jsonPath("$.resource_id").value(resourceId("GET", "/users/{id}")));
    }

    @Test
    void test_muc1_assignsPolicyToAResourceGroup() throws Exception {
        long reports = createAndGetId("/admin/api/resource-groups", Map.of("name", "reports"));

        long id = createAndGetId(ASSIGNMENTS, assignment("regular", "resource_group",
                "regular-100-per-minute", reports));

        adminGet(ASSIGNMENTS + "/{id}", id)
                .andExpect(jsonPath("$.relationship_type").value("resource_group"))
                .andExpect(jsonPath("$.resource_id").value(reports));
    }

    @Test
    void test_muc1_rejectsResourceAssignmentInsideAnAssignedResourceGroup() throws Exception {
        adminPost(ASSIGNMENTS, assignment("regular", "resource", "regular-100-per-minute",
                resourceId("GET", "/orders")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("belongs to resource group")))
                .andExpect(jsonPath("$.https_status_code").value(409));
    }

    @Test
    void test_muc1_rejectsGroupAssignmentWhenOneOfItsResourcesIsAssigned() throws Exception {
        adminPost(ASSIGNMENTS, assignment("free", "resource_group", "free-10-per-minute",
                resourceGroupId("orders")))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc1_rejectsUnknownReferencesAndTypes() throws Exception {
        adminPost(ASSIGNMENTS, assignment("free", "resource", "free-10-per-minute", 999_999L))
                .andExpect(status().isNotFound());
        adminPost(ASSIGNMENTS, Map.of("consumer_group_id", consumerGroupId("free"),
                "relationship_type", "everything", "policy_id", 1, "resource_id", 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test_muc2_listsAssignmentsOfOneConsumerGroup() throws Exception {
        adminGet(ASSIGNMENTS + "?consumer_group_id=" + consumerGroupId("regular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(3))
                .andExpect(jsonPath("$.content[0].relationship_type").value("resource_group"));
        adminGet(ASSIGNMENTS + "?consumer_group_id=" + consumerGroupId("free")
                + "&relationship_type=resource&size=5")
                .andExpect(jsonPath("$.total_elements").value(11))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void test_muc2_updatesAssignmentForAResourceGroup() throws Exception {
        long id = idOf("""
                select a.id from consumer_groups_policies a
                join consumer_groups g on g.id = a.consumer_group_id
                where g.group_name = 'regular' and a.resource_group_id = ?""",
                resourceGroupId("carts"));

        adminPut(ASSIGNMENTS + "/" + id, assignment("regular", "resource_group",
                "premium-500-per-minute", resourceGroupId("carts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_id").value(policyId("premium-500-per-minute")));
    }

    @Test
    void test_muc2_updateIsCheckedForConflicts() throws Exception {
        long id = idOf("""
                select a.id from consumer_groups_policies a
                join consumer_groups g on g.id = a.consumer_group_id
                where g.group_name = 'regular' and a.resource_group_id = ?""",
                resourceGroupId("carts"));

        adminPut(ASSIGNMENTS + "/" + id, assignment("regular", "resource_group",
                "regular-100-per-minute", resourceGroupId("orders")))
                .andExpect(status().isConflict());
    }

    @Test
    void test_muc2_deletesAssignment() throws Exception {
        long id = idOf("""
                select a.id from consumer_groups_policies a
                join consumer_groups g on g.id = a.consumer_group_id
                where g.group_name = 'premium' and a.resource_group_id = ?""",
                resourceGroupId("users"));

        adminDelete(ASSIGNMENTS + "/" + id).andExpect(status().isNoContent());
        adminGet(ASSIGNMENTS + "/{id}", id).andExpect(status().isNotFound());
    }

    private Map<String, Object> assignment(
            String group, String type, String policy, long targetId) {
        return Map.of("consumer_group_id", consumerGroupId(group), "relationship_type", type,
                "policy_id", policyId(policy), "resource_id", targetId);
    }
}
