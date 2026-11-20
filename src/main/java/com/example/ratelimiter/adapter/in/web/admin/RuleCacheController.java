package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.application.port.in.InvalidateRuleCacheUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MUC5: manual eviction of the applicable-rule cache (statistics: /actuator/caches, metrics). */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/cache")
public class RuleCacheController {

    private final InvalidateRuleCacheUseCase ruleCache;

    public RuleCacheController(InvalidateRuleCacheUseCase ruleCache) {
        this.ruleCache = ruleCache;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidate() {
        ruleCache.invalidateAll();
    }
}
