package com.example.ratelimiter.application.port.in.command;

import com.example.ratelimiter.domain.model.IoType;

/** Reclassifies a resource; method and path are its identity and cannot change. */
public record UpdateResourceCommand(IoType ioType, Long resourceGroupId) {
}
