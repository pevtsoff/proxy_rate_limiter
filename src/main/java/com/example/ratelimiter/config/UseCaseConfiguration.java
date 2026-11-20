package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.out.cache.CachingApplicableRuleResolver;
import com.example.ratelimiter.application.port.in.EvaluateRequestUseCase;
import com.example.ratelimiter.application.port.in.InvalidateRuleCacheUseCase;
import com.example.ratelimiter.application.port.in.IssueTokenUseCase;
import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManageConsumersUseCase;
import com.example.ratelimiter.application.port.in.ManagePoliciesUseCase;
import com.example.ratelimiter.application.port.in.ManagePolicyAssignmentsUseCase;
import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManageResourcesUseCase;
import com.example.ratelimiter.application.port.in.ProxyRequestUseCase;
import com.example.ratelimiter.application.port.in.ResolveApplicableRuleUseCase;
import com.example.ratelimiter.application.port.in.SyncResourcesUseCase;
import com.example.ratelimiter.application.port.out.ConsumerGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyAssignmentRepositoryPort;
import com.example.ratelimiter.application.port.out.PolicyRepositoryPort;
import com.example.ratelimiter.application.port.out.RateLimiterPort;
import com.example.ratelimiter.application.port.out.ResourceGroupRepositoryPort;
import com.example.ratelimiter.application.port.out.ResourceRepositoryPort;
import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.port.out.TargetApiCatalogPort;
import com.example.ratelimiter.application.port.out.TokenIssuerPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.application.port.out.UpstreamForwarderPort;
import com.example.ratelimiter.application.service.ApplicableRuleResolutionService;
import com.example.ratelimiter.application.service.ConsumerGroupService;
import com.example.ratelimiter.application.service.ConsumerService;
import com.example.ratelimiter.application.service.EvaluateRequestService;
import com.example.ratelimiter.application.service.PolicyAssignmentService;
import com.example.ratelimiter.application.service.PolicyService;
import com.example.ratelimiter.application.service.ProxyRequestService;
import com.example.ratelimiter.application.service.ResourceGroupService;
import com.example.ratelimiter.application.service.ResourceService;
import com.example.ratelimiter.application.service.ResourceSyncService;
import com.example.ratelimiter.application.service.RuleAffectingTransaction;
import com.example.ratelimiter.application.service.RuleCacheService;
import com.example.ratelimiter.application.service.TokenService;
import com.example.ratelimiter.domain.service.AssignmentConflictDetector;
import com.example.ratelimiter.domain.service.AssignmentSelector;
import com.example.ratelimiter.domain.service.ResourcePathMatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Composition root of the hexagon: the framework-free domain and application classes are
 * instantiated here and wired to the adapters through their ports.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfiguration {

    @Bean
    public ResourcePathMatcher resourcePathMatcher() {
        return new ResourcePathMatcher();
    }

    @Bean
    public AssignmentSelector assignmentSelector() {
        return new AssignmentSelector();
    }

    @Bean
    public AssignmentConflictDetector assignmentConflictDetector() {
        return new AssignmentConflictDetector();
    }

    @Bean
    public RuleAffectingTransaction ruleAffectingTransaction(
            UnitOfWorkPort unitOfWork, RuleCacheInvalidationPort ruleCache) {
        return new RuleAffectingTransaction(unitOfWork, ruleCache);
    }

    @Bean
    public ManagePoliciesUseCase managePoliciesUseCase(PolicyRepositoryPort policies,
            PolicyAssignmentRepositoryPort assignments, UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction transaction) {
        return new PolicyService(policies, assignments, unitOfWork, transaction);
    }

    @Bean
    public ManageConsumerGroupsUseCase manageConsumerGroupsUseCase(
            ConsumerGroupRepositoryPort groups, ConsumerRepositoryPort consumers,
            PolicyAssignmentRepositoryPort assignments, UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction transaction) {
        return new ConsumerGroupService(groups, consumers, assignments, unitOfWork, transaction);
    }

    @Bean
    public ManageConsumersUseCase manageConsumersUseCase(ConsumerRepositoryPort consumers,
            ConsumerGroupRepositoryPort groups, UnitOfWorkPort unitOfWork,
            RuleAffectingTransaction transaction) {
        return new ConsumerService(consumers, groups, unitOfWork, transaction);
    }

    @Bean
    public ManageResourceGroupsUseCase manageResourceGroupsUseCase(
            ResourceGroupRepositoryPort resourceGroups, PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork, RuleAffectingTransaction transaction) {
        return new ResourceGroupService(resourceGroups, assignments, unitOfWork, transaction);
    }

    @Bean
    public ManageResourcesUseCase manageResourcesUseCase(ResourceRepositoryPort resources,
            ResourceGroupRepositoryPort resourceGroups, PolicyAssignmentRepositoryPort assignments,
            UnitOfWorkPort unitOfWork, RuleAffectingTransaction transaction) {
        return new ResourceService(resources, resourceGroups, assignments, unitOfWork, transaction);
    }

    @Bean
    public SyncResourcesUseCase syncResourcesUseCase(TargetApiCatalogPort catalog,
            ResourceRepositoryPort resources, ResourceGroupRepositoryPort resourceGroups,
            RuleAffectingTransaction transaction) {
        return new ResourceSyncService(catalog, resources, resourceGroups, transaction);
    }

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ManagePolicyAssignmentsUseCase managePolicyAssignmentsUseCase(
            PolicyAssignmentRepositoryPort assignments, ConsumerGroupRepositoryPort groups,
            PolicyRepositoryPort policies, ResourceRepositoryPort resources,
            ResourceGroupRepositoryPort resourceGroups, AssignmentConflictDetector detector,
            UnitOfWorkPort unitOfWork, RuleAffectingTransaction transaction) {
        return new PolicyAssignmentService(assignments, groups, policies, resources,
                resourceGroups, detector, unitOfWork, transaction);
    }

    @Bean
    public InvalidateRuleCacheUseCase invalidateRuleCacheUseCase(
            RuleCacheInvalidationPort ruleCache) {
        return new RuleCacheService(ruleCache);
    }

    @Bean
    public IssueTokenUseCase issueTokenUseCase(ConsumerRepositoryPort consumers,
            TokenIssuerPort tokenIssuer, UnitOfWorkPort unitOfWork) {
        return new TokenService(consumers, tokenIssuer, unitOfWork);
    }

    /** Uncached resolution straight from PostgreSQL. */
    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ApplicableRuleResolutionService databaseRuleResolver(ConsumerRepositoryPort consumers,
            ResourceRepositoryPort resources, PolicyAssignmentRepositoryPort assignments,
            PolicyRepositoryPort policies, ResourcePathMatcher matcher,
            AssignmentSelector selector, UnitOfWorkPort unitOfWork) {
        return new ApplicableRuleResolutionService(
                consumers, resources, assignments, policies, matcher, selector, unitOfWork);
    }

    /** MUC5: the resolver every caller gets is the cached decorator. */
    @Bean
    @Primary
    public ResolveApplicableRuleUseCase cachedRuleResolver(
            @Qualifier("databaseRuleResolver")
            ApplicableRuleResolutionService databaseRuleResolver) {
        return new CachingApplicableRuleResolver(databaseRuleResolver);
    }

    @Bean
    public EvaluateRequestUseCase evaluateRequestUseCase(
            ResolveApplicableRuleUseCase ruleResolver, RateLimiterPort rateLimiter) {
        return new EvaluateRequestService(ruleResolver, rateLimiter);
    }

    @Bean
    public ProxyRequestUseCase proxyRequestUseCase(
            EvaluateRequestUseCase evaluator, UpstreamForwarderPort forwarder) {
        return new ProxyRequestService(evaluator, forwarder);
    }
}
