package com.example.ratelimiter.adapter.out.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small builder that turns optional filter values into a JPA {@link Specification}: criteria
 * whose value is {@code null} (or blank) are skipped.
 */
final class Specifications<E> {

    private final List<Criterion<E>> criteria = new ArrayList<>();

    @FunctionalInterface
    private interface Criterion<E> {
        Predicate toPredicate(Root<E> root, CriteriaBuilder builder);
    }

    Specifications<E> equal(String attribute, Object value) {
        if (value != null) {
            criteria.add((root, builder) -> builder.equal(root.get(attribute), value));
        }
        return this;
    }

    Specifications<E> containsIgnoreCase(String attribute, String value) {
        if (value != null && !value.isBlank()) {
            String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
            criteria.add((root, builder) ->
                    builder.like(builder.lower(root.<String>get(attribute)), pattern));
        }
        return this;
    }

    Specification<E> build() {
        List<Criterion<E>> snapshot = List.copyOf(criteria);
        return (root, query, builder) -> builder.and(snapshot.stream()
                .map(criterion -> criterion.toPredicate(root, builder))
                .toArray(Predicate[]::new));
    }
}
