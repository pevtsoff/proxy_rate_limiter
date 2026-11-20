package com.example.ratelimiter.application.port.out;

import com.example.ratelimiter.domain.model.DiscoveredEndpoint;
import java.util.List;

/** Source of the target API's endpoint list (its OpenAPI document). */
public interface TargetApiCatalogPort {

    List<DiscoveredEndpoint> fetchEndpoints();
}
