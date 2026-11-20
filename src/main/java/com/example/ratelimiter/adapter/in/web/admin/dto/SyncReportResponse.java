package com.example.ratelimiter.adapter.in.web.admin.dto;

import com.example.ratelimiter.domain.model.ResourceSyncReport;
import java.util.List;

/** Result of {@code POST /admin/api/resources/sync}. */
public record SyncReportResponse(
        List<ResourceResponse> created, int unchanged, List<ResourceResponse> stale) {

    public static SyncReportResponse from(ResourceSyncReport report) {
        return new SyncReportResponse(
                report.created().stream().map(ResourceResponse::from).toList(),
                report.unchanged(),
                report.stale().stream().map(ResourceResponse::from).toList());
    }
}
