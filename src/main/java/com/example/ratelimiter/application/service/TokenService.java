package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.in.IssueTokenUseCase;
import com.example.ratelimiter.application.port.out.ConsumerRepositoryPort;
import com.example.ratelimiter.application.port.out.TokenIssuerPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import com.example.ratelimiter.domain.exception.NotFoundException;
import com.example.ratelimiter.domain.model.IssuedToken;

/** Issues tokens only for registered consumers. */
public final class TokenService implements IssueTokenUseCase {

    private final ConsumerRepositoryPort consumers;
    private final TokenIssuerPort tokenIssuer;
    private final UnitOfWorkPort unitOfWork;

    public TokenService(
            ConsumerRepositoryPort consumers, TokenIssuerPort tokenIssuer,
                    UnitOfWorkPort unitOfWork) {
        this.consumers = consumers;
        this.tokenIssuer = tokenIssuer;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public IssuedToken issue(String consumerId) {
        unitOfWork.inReadOnlyTransaction(() -> consumers.findByConsumerId(consumerId))
                .orElseThrow(() -> new NotFoundException("Consumer", consumerId));
        return tokenIssuer.issue(consumerId);
    }
}
