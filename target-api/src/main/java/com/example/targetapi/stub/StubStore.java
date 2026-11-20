package com.example.targetapi.stub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory behaviour of one stub resource:
 * <ul>
 *   <li>GET and PUT are idempotent - repeating them yields the same state;</li>
 *   <li>POST creates a new id every time and PATCH bumps a revision counter - repeating them
 *       changes state, i.e. they are not idempotent.</li>
 * </ul>
 */
public class StubStore {

    private static final Map<String, Object> STATIC_DATA = Map.of("status", "ok", "stub", true);

    private final String resource;
    private final AtomicLong sequence = new AtomicLong(100);
    private final ConcurrentMap<Long, AtomicLong> revisions = new ConcurrentHashMap<>();

    public StubStore(String resource) {
        this.resource = resource;
    }

    public List<StubRecord> list() {
        return List.of(get(1), get(2));
    }

    public StubRecord get(long id) {
        return new StubRecord(resource, id, currentRevision(id), STATIC_DATA);
    }

    public StubRecord create(Map<String, Object> data) {
        long id = sequence.incrementAndGet();
        return new StubRecord(resource, id, 0, dataOrStatic(data));
    }

    public StubRecord replace(long id, Map<String, Object> data) {
        return new StubRecord(resource, id, currentRevision(id), dataOrStatic(data));
    }

    public StubRecord patch(long id, Map<String, Object> data) {
        long revision = revisions.computeIfAbsent(id, key -> new AtomicLong()).incrementAndGet();
        return new StubRecord(resource, id, revision, dataOrStatic(data));
    }

    private long currentRevision(long id) {
        AtomicLong revision = revisions.get(id);
        return revision == null ? 0 : revision.get();
    }

    private static Map<String, Object> dataOrStatic(Map<String, Object> data) {
        return data == null || data.isEmpty() ? STATIC_DATA : data;
    }
}
