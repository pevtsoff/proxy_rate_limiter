package com.example.targetapi.stub;

import java.util.Map;

/**
 * Static representation returned for every resource item.
 *
 * @param revision incremented by each PATCH, which is what makes PATCH non-idempotent
 */
public record StubRecord(String resource, long id, long revision, Map<String, Object> data) {
}
