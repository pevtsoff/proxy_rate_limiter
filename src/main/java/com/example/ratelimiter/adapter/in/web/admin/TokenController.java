package com.example.ratelimiter.adapter.in.web.admin;

import com.example.ratelimiter.adapter.in.web.admin.dto.TokenRequest;
import com.example.ratelimiter.adapter.in.web.admin.dto.TokenResponse;
import com.example.ratelimiter.application.port.in.IssueTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development helper standing in for an identity provider: issues HS256 bearer tokens for
 * registered consumers.
 */
@RestController
@RequestMapping(AdminApi.BASE_PATH + "/tokens")
public class TokenController {

    private final IssueTokenUseCase tokens;

    public TokenController(IssueTokenUseCase tokens) {
        this.tokens = tokens;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse issue(@Valid @RequestBody TokenRequest request) {
        return TokenResponse.from(tokens.issue(request.consumerId()));
    }
}
