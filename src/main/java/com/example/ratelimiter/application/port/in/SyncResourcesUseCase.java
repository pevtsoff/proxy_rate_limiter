package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.ResourceSyncReport;

/** MUC1.4: imports every endpoint advertised in the target API's OpenAPI document. */
public interface SyncResourcesUseCase {

    ResourceSyncReport synchronize();
}
