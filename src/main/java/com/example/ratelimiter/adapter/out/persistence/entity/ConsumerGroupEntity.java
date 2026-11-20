package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.adapter.out.persistence.converter.ConsumerGroupNameConverter;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of {@code consumer_groups}. */
@Entity
@Table(name = "consumer_groups")
public class ConsumerGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = ConsumerGroupNameConverter.class)
    @Column(name = "group_name", nullable = false, length = 16)
    private ConsumerGroupName name;

    protected ConsumerGroupEntity() {
    }

    public static ConsumerGroupEntity from(ConsumerGroup group) {
        ConsumerGroupEntity entity = new ConsumerGroupEntity();
        entity.apply(group);
        return entity;
    }

    public void apply(ConsumerGroup group) {
        this.name = group.name();
    }

    public ConsumerGroup toDomain() {
        return new ConsumerGroup(id, name);
    }
}
