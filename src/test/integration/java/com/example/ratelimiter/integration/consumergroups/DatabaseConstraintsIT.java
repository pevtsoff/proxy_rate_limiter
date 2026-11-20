package com.example.ratelimiter.integration.consumergroups;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ratelimiter.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The integrity rules hold even for writes that bypass the application. These tests commit, so
 * they are not {@code @Transactional}; data is cleaned up explicitly after each test.
 */
@Sql(IntegrationTest.SEED)
@Sql(scripts = IntegrationTest.CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DatabaseConstraintsIT extends IntegrationTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void test_muc1_databaseRejectsAnEmptyConsumerGroupAtCommit() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        jdbc.update("delete from consumer_groups where group_name = 'internal'");

        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                jdbc.update("insert into consumer_groups (group_name) values ('internal')")))
                .hasStackTraceContaining("must have at least one consumer");
        assertThatCode(() -> transaction.executeWithoutResult(status -> {
            jdbc.update("insert into consumer_groups (group_name) values ('internal')");
            jdbc.update("insert into consumers (consumer_id, consumer_group_id) "
                    + "select 'svc', id from consumer_groups where group_name = 'internal'");
        })).doesNotThrowAnyException();
    }

    @Test
    void test_muc1_databaseRejectsDeletingTheLastConsumer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                jdbc.update("delete from consumers where consumer_id = 'internal-service-1'")))
                .hasStackTraceContaining("must have at least one consumer");
    }

    @Test
    void test_muc1_databaseRejectsOverlappingAssignments() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into consumer_groups_policies
                    (consumer_group_id, relationship_type, policy_id, resource_id)
                values (?, 'resource', ?, ?)""",
                consumerGroupId("regular"), policyId("regular-100-per-minute"),
                resourceId("GET", "/orders")))
                .hasStackTraceContaining("is covered by an assigned resource group");
        assertThatThrownBy(() -> jdbc.update("""
                insert into consumer_groups_policies
                    (consumer_group_id, relationship_type, policy_id, resource_id,
                     resource_group_id)
                values (?, 'resource', ?, ?, ?)""",
                consumerGroupId("free"), policyId("free-10-per-minute"),
                resourceId("GET", "/users"), resourceGroupId("users")))
                .hasStackTraceContaining("cgp_exactly_one_target");
    }
}
