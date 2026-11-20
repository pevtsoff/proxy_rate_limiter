package com.example.ratelimiter.application.port.in;

import com.example.ratelimiter.domain.model.IssuedToken;

/** Development helper: mints a bearer token for a registered consumer. */
public interface IssueTokenUseCase {

    IssuedToken issue(String consumerId);
}
