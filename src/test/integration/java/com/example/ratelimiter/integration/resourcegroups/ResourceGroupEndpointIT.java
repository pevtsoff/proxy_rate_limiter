package com.example.ratelimiter.integration.resourcegroups;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ratelimiter.integration.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** /admin/api/resource-groups. */
@Transactional
@Sql(IntegrationTest.SEED)
class ResourceGroupEndpointIT extends IntegrationTest {

    private static final String GROUPS = "/admin/api/resource-groups";

    @Test
    void test_muc1_createsRenamesAndDeletesResourceGroup() throws Exception {
        long id = createAndGetId(GROUPS, Map.of("name", "reports"));
        adminPut(GROUPS + "/" + id, Map.of("name", "analytics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("analytics"));
        adminDelete(GROUPS + "/" + id).andExpect(status().isNoContent());
        adminGet(GROUPS + "/{id}", id).andExpect(status().isNotFound());
    }

    @Test
    void test_muc1_rejectsDuplicateAndBlankNames() throws Exception {
        adminPost(GROUPS, Map.of("name", "orders")).andExpect(status().isConflict());
        adminPost(GROUPS, Map.of("name", " ")).andExpect(status().isBadRequest());
    }

    @Test
    void test_muc1_assignedResourceGroupCannotBeDeleted() throws Exception {
        adminDelete(GROUPS + "/" + resourceGroupId("orders")).andExpect(status().isConflict());
    }

    @Test
    void test_muc1_listsResourceGroupsWithPagination() throws Exception {
        adminGet(GROUPS + "?size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(3))
                .andExpect(jsonPath("$.total_pages").value(2));
    }
}
