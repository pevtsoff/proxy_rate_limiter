package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.ResolveApplicableRuleUseCase;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.exception.UnknownConsumerException;
import com.example.ratelimiter.domain.model.ApplicableRule;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.service.AssignmentSelector;
import com.example.ratelimiter.domain.service.ResourcePathMatcher;

/**
 * Reads the configuration database to find the policy for {@code (consumer, method, path)}:
 * consumer -> consumer group -> matching resource -> resource or resource-group assignment ->
 * policy, falling back to the default policy (MUC3).
 *
 * <p>This class is deliberately uncached; caching is added by a decorator in the adapter layer.
 */
public final class ApplicableRuleResolutionService implements ResolveApplicableRuleUseCase {

    private final ConsumerRepositoryPort consumers;
    private final ResourceRepositoryPort resources;
    private final PolicyAssignmentRepositoryPort assignments;
    private final PolicyRepositoryPort policies;
    private final ResourcePathMatcher pathMatcher;
    private final AssignmentSelector assignmentSelector;
    private final UnitOfWorkPort unitOfWork;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public ApplicableRuleResolutionService(
            ConsumerRepositoryPort consumers,
            ResourceRepositoryPort resources,
            PolicyAssignmentRepositoryPort assignments,
            PolicyRepositoryPort policies,
            ResourcePathMatcher pathMatcher,
            AssignmentSelector assignmentSelector,
            UnitOfWorkPort unitOfWork) {
        this.consumers = consumers;
        this.resources = resources;
        this.assignments = assignments;
        this.policies = policies;
        this.pathMatcher = pathMatcher;
        this.assignmentSelector = assignmentSelector;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public ApplicableRule resolve(String consumerId, HttpMethod method, String path) {
        return unitOfWork.inReadOnlyTransaction(() -> resolveFromDatabase(consumerId, method,
                path));
    }

    private ApplicableRule resolveFromDatabase(String consumerId, HttpMethod method, String path) {
        Consumer consumer = consumers.findByConsumerId(consumerId)
                .orElseThrow(() -> new UnknownConsumerException(consumerId));
        return pathMatcher.match(resources.findAll(), method, path)
                .map(resource -> ruleForResource(consumer, resource))
                .orElseGet(() -> ApplicableRule.defaultForUnmatched(defaultPolicy()));
    }

    private ApplicableRule ruleForResource(Consumer consumer, Resource resource) {
        return assignmentSelector
                .select(resource, assignments.findByConsumerGroupId(consumer.consumerGroupId()))
                .map(assignment -> ruleForAssignment(assignment, resource))
                .orElseGet(() -> ApplicableRule.defaultForResource(defaultPolicy(), resource));
    }

    private ApplicableRule ruleForAssignment(PolicyAssignment assignment, Resource resource) {
        Policy policy = policies.findById(assignment.policyId())
                .orElseThrow(() -> new NotFoundException("Policy", assignment.policyId()));
        return assignment.relationshipType() == RelationshipType.RESOURCE
                ? ApplicableRule.forResource(policy, resource)
                : ApplicableRule.forResourceGroup(policy, assignment.targetId());
    }

    private Policy defaultPolicy() {
        return policies.findDefault().orElseThrow(() -> new IllegalStateException(
                "Default policy is missing; it is created by Flyway migration V2"));
    }
}
