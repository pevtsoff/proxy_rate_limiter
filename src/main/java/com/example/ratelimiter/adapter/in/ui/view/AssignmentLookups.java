package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManagePoliciesUseCase;
import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManageResourcesUseCase;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.PolicyFilter;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.ResourceFilter;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reference data the assignment view needs to render ids as names and to offer choices. Kept
 * separate from the view so the view only deals with layout (single responsibility).
 */
@Component
@UIScope
public class AssignmentLookups {

    /** Selectable target of an assignment: a resource or a resource group. */
    public record Target(Long id, String label) {
    }

    private final ManageConsumerGroupsUseCase consumerGroups;
    private final ManagePoliciesUseCase policies;
    private final ManageResourcesUseCase resources;
    private final ManageResourceGroupsUseCase resourceGroups;
    private Map<Long, ConsumerGroup> groupsById = Map.of();
    private Map<Long, Policy> policiesById = Map.of();
    private Map<Long, Target> resourcesById = Map.of();
    private Map<Long, Target> resourceGroupsById = Map.of();

    public AssignmentLookups(ManageConsumerGroupsUseCase consumerGroups,
            ManagePoliciesUseCase policies, ManageResourcesUseCase resources,
            ManageResourceGroupsUseCase resourceGroups) {
        this.consumerGroups = consumerGroups;
        this.policies = policies;
        this.resources = resources;
        this.resourceGroups = resourceGroups;
    }

    public void reload() {
        groupsById = index(consumerGroups.list(PageQuery.all()).content(), ConsumerGroup::id);
        policiesById = index(policies.list(PolicyFilter.none(), PageQuery.all()).content(),
                Policy::id);
        resourcesById = index(resources.list(ResourceFilter.none(), PageQuery.all()).content()
                .stream().map(r -> new Target(r.id(), r.label())).toList(), Target::id);
        resourceGroupsById = index(resourceGroups.list(PageQuery.all()).content().stream()
                .map(g -> new Target(g.id(), g.name())).toList(), Target::id);
    }

    public Collection<ConsumerGroup> groups() {
        return groupsById.values();
    }

    public Collection<Policy> policies() {
        return policiesById.values();
    }

    public List<Target> targets(RelationshipType type) {
        return List.copyOf(targetsOf(type).values());
    }

    public ConsumerGroup group(Long id) {
        return groupsById.get(id);
    }

    public Policy policy(Long id) {
        return policiesById.get(id);
    }

    public Target target(RelationshipType type, Long id) {
        return targetsOf(type).get(id);
    }

    public String groupLabel(Long id) {
        ConsumerGroup group = groupsById.get(id);
        return group == null ? "#" + id : group.name().value();
    }

    public String policyLabel(Long id) {
        Policy policy = policiesById.get(id);
        return policy == null ? "#" + id : policy.name();
    }

    public String targetLabel(PolicyAssignment assignment) {
        Target target = target(assignment.relationshipType(), assignment.targetId());
        return target == null ? "#" + assignment.targetId() : target.label();
    }

    private Map<Long, Target> targetsOf(RelationshipType type) {
        return type == RelationshipType.RESOURCE_GROUP ? resourceGroupsById : resourcesById;
    }

    private static <T> Map<Long, T> index(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity(),
                (first, second) -> first, LinkedHashMap::new));
    }
}
