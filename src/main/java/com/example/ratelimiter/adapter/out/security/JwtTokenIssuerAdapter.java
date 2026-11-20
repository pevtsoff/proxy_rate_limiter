package com.example.ratelimiter.adapter.out.security;

import com.example.ratelimiter.application.port.out.TokenIssuerPort;
import com.example.ratelimiter.domain.model.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Signs HS256 JWTs whose {@code sub} claim is the consumer id. */
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    static final String ISSUER = "proxy-rate-limiter";

    private final JwtEncoder encoder;
    private final Duration timeToLive;
    private final Clock clock;

    public JwtTokenIssuerAdapter(JwtEncoder encoder, Duration timeToLive, Clock clock) {
        this.encoder = encoder;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(String consumerId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(timeToLive);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(consumerId)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(consumerId, token, expiresAt);
    }
}
