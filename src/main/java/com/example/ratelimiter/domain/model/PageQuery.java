package com.example.ratelimiter.domain.model;

import com.example.ratelimiter.domain.exception.ValidationException;

/** Zero-based page request, independent of any persistence framework. */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 500;

    public PageQuery {
        if (page < 0) {
            throw new ValidationException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageQuery first(int size) {
        return new PageQuery(0, size);
    }

    /** Convenience page used by UI selectors that need the complete (small) list. */
    public static PageQuery all() {
        return new PageQuery(0, MAX_SIZE);
    }
}
