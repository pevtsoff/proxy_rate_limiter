package com.example.ratelimiter.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.port.out.TargetApiCatalogPort;
import com.example.ratelimiter.application.service.ResourceSyncService;
import com.example.ratelimiter.application.service.RuleAffectingTransaction;
import com.example.ratelimiter.domain.model.DiscoveredEndpoint;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceGroup;
import com.example.ratelimiter.domain.model.ResourceSyncReport;
import com.example.ratelimiter.unit.support.DirectUnitOfWork;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MUC1.4: idempotent import with default io types and resource groups. */
@ExtendWith(MockitoExtension.class)
class ResourceSyncServiceTest {

    @Mock
    private TargetApiCatalogPort catalog;
    @Mock
    private ResourceRepositoryPort resources;
    @Mock
    private ResourceGroupRepositoryPort resourceGroups;
    @Mock
    private RuleCacheInvalidationPort ruleCache;

    @Test
    void importsOnlyNewEndpointsAndReportsStaleOnes() {
        Resource existing = new Resource(1L, HttpMethod.GET, "/orders", IoType.READ, 5L);
        Resource stale = new Resource(2L, HttpMethod.GET, "/legacy", IoType.READ, null);
        when(catalog.fetchEndpoints()).thenReturn(List.of(
                new DiscoveredEndpoint(HttpMethod.GET, "/orders"),
                new DiscoveredEndpoint(HttpMethod.PATCH, "/orders/{id}")));
        when(resources.findAll()).thenReturn(List.of(existing, stale));
        when(resourceGroups.findByName("orders"))
                .thenReturn(Optional.of(new ResourceGroup(5L, "orders")));
        when(resources.save(any(Resource.class))).thenAnswer(call -> call.getArgument(0));

        ResourceSyncReport report = service().synchronize();

        assertThat(report.created()).singleElement()
                .satisfies(created -> {
                    assertThat(created.path()).isEqualTo("/orders/{id}");
                    assertThat(created.ioType()).isEqualTo(IoType.WRITE);
                    assertThat(created.resourceGroupId()).isEqualTo(5L);
                });
        assertThat(report.unchanged()).isEqualTo(1);
        assertThat(report.stale()).containsExactly(stale);
        verify(ruleCache).invalidateAll();
    }

    private ResourceSyncService service() {
        DirectUnitOfWork unitOfWork = new DirectUnitOfWork();
        return new ResourceSyncService(catalog, resources, resourceGroups,
                new RuleAffectingTransaction(unitOfWork, ruleCache));
    }
}
