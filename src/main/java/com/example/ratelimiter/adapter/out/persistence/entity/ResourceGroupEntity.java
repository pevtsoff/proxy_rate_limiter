package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.domain.model.ResourceGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of {@code resource_groups}. */
@Entity
@Table(name = "resource_groups")
public class ResourceGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected ResourceGroupEntity() {
    }

    public static ResourceGroupEntity from(ResourceGroup group) {
        ResourceGroupEntity entity = new ResourceGroupEntity();
        entity.apply(group);
        return entity;
    }

    public void apply(ResourceGroup group) {
        this.name = group.name();
    }

    public ResourceGroup toDomain() {
        return new ResourceGroup(id, name);
    }
}
