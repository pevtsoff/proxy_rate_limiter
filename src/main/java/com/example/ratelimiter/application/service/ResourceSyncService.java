package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.SyncResourcesUseCase;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.TargetApiCatalogPort;
import com.example.ratelimiter.domain.model.DiscoveredEndpoint;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceGroup;
import com.example.ratelimiter.domain.model.ResourcePaths;
import com.example.ratelimiter.domain.model.ResourceSyncReport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports target endpoints idempotently. New endpoints are classified by HTTP method and placed
 * into a resource group named after their first path segment ({@code /orders/{id}} goes to
 * {@code orders}). Existing resources are never overwritten, so manual re-classification
 * survives a re-sync.
 */
public final class ResourceSyncService implements SyncResourcesUseCase {

    private final TargetApiCatalogPort catalog;
    private final ResourceRepositoryPort resources;
    private final ResourceGroupRepositoryPort resourceGroups;
    private final RuleAffectingTransaction ruleAffectingTransaction;

    public ResourceSyncService(
            TargetApiCatalogPort catalog,
            ResourceRepositoryPort resources,
            ResourceGroupRepositoryPort resourceGroups,
            RuleAffectingTransaction ruleAffectingTransaction) {
        this.catalog = catalog;
        this.resources = resources;
        this.resourceGroups = resourceGroups;
        this.ruleAffectingTransaction = ruleAffectingTransaction;
    }

    @Override
    public ResourceSyncReport synchronize() {
        List<DiscoveredEndpoint> endpoints = catalog.fetchEndpoints();
        return ruleAffectingTransaction.execute(() -> importEndpoints(endpoints));
    }

    private ResourceSyncReport importEndpoints(List<DiscoveredEndpoint> endpoints) {
        List<Resource> known = resources.findAll();
        Map<String, ResourceGroup> groupsByName = new HashMap<>();
        List<Resource> created = new ArrayList<>();
        for (DiscoveredEndpoint endpoint : endpoints) {
            if (isKnown(endpoint, known)) {
                continue;
            }
            ResourceGroup group = groupFor(endpoint, groupsByName);
            created.add(resources.save(
                    Resource.discovered(endpoint.method(), endpoint.path(), group.id())));
        }
        return new ResourceSyncReport(created, endpoints.size() - created.size(),
                staleResources(known, endpoints));
    }

    private static boolean isKnown(DiscoveredEndpoint endpoint, List<Resource> known) {
        return known.stream().anyMatch(endpoint::sameEndpointAs);
    }

    private ResourceGroup groupFor(DiscoveredEndpoint endpoint, Map<String, ResourceGroup> cache) {
        String name = ResourcePaths.firstSegment(endpoint.path());
        return cache.computeIfAbsent(name, key -> resourceGroups.findByName(key)
                .orElseGet(() -> resourceGroups.save(ResourceGroup.newGroup(key))));
    }

    private static List<Resource> staleResources(
            List<Resource> known, List<DiscoveredEndpoint> endpoints) {
        return known.stream()
                .filter(resource -> endpoints.stream()
                        .noneMatch(endpoint -> endpoint.sameEndpointAs(resource)))
                .toList();
    }
}
