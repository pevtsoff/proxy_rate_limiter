package com.example.ratelimiter.domain.model;

import java.util.List;

/**
 * Result of synchronising the resource catalogue with the target's OpenAPI document.
 *
 * @param created   resources that were new and have been inserted
 * @param unchanged number of discovered resources that already existed
 * @param stale     known resources no longer advertised by the target (kept, not deleted,
 *                  because assignments may still reference them)
 */
public record ResourceSyncReport(List<Resource> created, int unchanged, List<Resource> stale) {

    public ResourceSyncReport {
        created = List.copyOf(created);
        stale = List.copyOf(stale);
    }
}
