package com.example.ratelimiter.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ratelimiter.adapter.in.web.admin.dto.PolicyRequest;
import com.example.ratelimiter.adapter.in.web.json.CodedEnumJacksonModule;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.example.ratelimiter.domain.model.TimePeriod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The REST contract uses external codes such as "moving-window" and snake_case fields. */
class CodedEnumJacksonModuleTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new CodedEnumJacksonModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void serialisesEnumsByCode() throws Exception {
        assertThat(mapper.writeValueAsString(
                List.of(RateLimitingStrategy.MOVING_WINDOW, RelationshipType.RESOURCE_GROUP)))
                .isEqualTo("[\"moving-window\",\"resource_group\"]");
    }

    @Test
    void deserialisesSnakeCaseRequestWithCodes() throws Exception {
        PolicyRequest request = mapper.readValue("""
                {"name": "p", "rate_limiting_strategy": "token-bucket",
                 "number_of_requests": 5, "time_period": "hour"}
                """, PolicyRequest.class);

        assertThat(request.rateLimitingStrategy()).isEqualTo(RateLimitingStrategy.TOKEN_BUCKET);
        assertThat(request.timePeriod()).isEqualTo(TimePeriod.HOUR);
        assertThat(request.numberOfRequests()).isEqualTo(5);
    }

    @Test
    void rejectsUnknownCodes() {
        assertThatThrownBy(() -> mapper.readValue(
                "{\"name\":\"p\",\"number_of_requests\":1,\"time_period\":\"fortnight\"}",
                PolicyRequest.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("second, minute, hour, day, week");
    }
}
