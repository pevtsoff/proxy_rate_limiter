package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.adapter.out.persistence.converter.HttpMethodConverter;
import com.example.ratelimiter.adapter.out.persistence.converter.IoTypeConverter;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of {@code resources}. */
@Entity
@Table(name = "resources")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = HttpMethodConverter.class)
    @Column(name = "method", nullable = false, length = 10, updatable = false)
    private HttpMethod method;

    @Column(name = "resource_path", nullable = false, length = 512, updatable = false)
    private String path;

    @Convert(converter = IoTypeConverter.class)
    @Column(name = "io_type", nullable = false, length = 8)
    private IoType ioType;

    @Column(name = "resource_group_id")
    private Long resourceGroupId;

    protected ResourceEntity() {
    }

    public static ResourceEntity from(Resource resource) {
        ResourceEntity entity = new ResourceEntity();
        entity.method = resource.method();
        entity.path = resource.path();
        entity.apply(resource);
        return entity;
    }

    /** Copies the mutable classification; method and path are the resource's identity. */
    public void apply(Resource resource) {
        this.ioType = resource.ioType();
        this.resourceGroupId = resource.resourceGroupId();
    }

    public Resource toDomain() {
        return new Resource(id, method, path, ioType, resourceGroupId);
    }
}
