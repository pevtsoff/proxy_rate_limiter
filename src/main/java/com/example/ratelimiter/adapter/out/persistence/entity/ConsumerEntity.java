package com.example.ratelimiter.adapter.out.persistence.entity;

import com.example.ratelimiter.domain.model.Consumer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA mapping of {@code consumers}. */
@Entity
@Table(name = "consumers")
public class ConsumerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_id", nullable = false, length = 128)
    private String consumerId;

    @Column(name = "consumer_group_id", nullable = false)
    private Long consumerGroupId;

    protected ConsumerEntity() {
    }

    public static ConsumerEntity from(Consumer consumer) {
        ConsumerEntity entity = new ConsumerEntity();
        entity.apply(consumer);
        return entity;
    }

    public void apply(Consumer consumer) {
        this.consumerId = consumer.consumerId();
        this.consumerGroupId = consumer.consumerGroupId();
    }

    public Consumer toDomain() {
        return new Consumer(id, consumerId, consumerGroupId);
    }
}
