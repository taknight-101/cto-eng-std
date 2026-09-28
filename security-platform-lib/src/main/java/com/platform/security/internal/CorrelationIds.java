package com.platform.security.internal;

import java.util.UUID;

/** Correlation/request identifier generation, centralized so the format can evolve in one place. */
public final class CorrelationIds {

    private CorrelationIds() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
