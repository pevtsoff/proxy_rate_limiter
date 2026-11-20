package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.adapter.out.persistence.converter.RelationshipTypeConverter;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping of {@code consumer_groups_policies}. The domain's single {@code targetId} is split
 * into two foreign-key columns according to {@code relationship_type}.
 */
@Entity
@Table(name = "consumer_groups_policies")
public class PolicyAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_group_id", nullable = false)
    private Long consumerGroupId;

    @Convert(converter = RelationshipTypeConverter.class)
    @Column(name = "relationship_type", nullable = false, length = 16)
    private RelationshipType relationshipType;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_group_id")
    private Long resourceGroupId;

    protected PolicyAssignmentEntity() {
    }

    public static PolicyAssignmentEntity from(PolicyAssignment assignment) {
        PolicyAssignmentEntity entity = new PolicyAssignmentEntity();
        entity.apply(assignment);
        return entity;
    }

    public void apply(PolicyAssignment assignment) {
        this.consumerGroupId = assignment.consumerGroupId();
        this.relationshipType = assignment.relationshipType();
        this.policyId = assignment.policyId();
        boolean targetsResource = assignment.relationshipType() == RelationshipType.RESOURCE;
        this.resourceId = targetsResource ? assignment.targetId() : null;
        this.resourceGroupId = targetsResource ? null : assignment.targetId();
    }

    public PolicyAssignment toDomain() {
        Long targetId = relationshipType == RelationshipType.RESOURCE
                ? resourceId : resourceGroupId;
        return new PolicyAssignment(id, consumerGroupId, relationshipType, policyId, targetId);
    }
}
