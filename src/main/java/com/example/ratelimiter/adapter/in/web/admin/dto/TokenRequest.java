package com.example.ratelimiter.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** Request for a development bearer token. */
public record TokenRequest(@NotBlank String consumerId) {
}
