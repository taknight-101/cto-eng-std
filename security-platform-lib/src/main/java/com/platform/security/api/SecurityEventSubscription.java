package com.platform.security.api;

/** Handle returned by {@link SecurityEventDrill#subscribe}, allowing a consumer to detach. */
@FunctionalInterface
public interface SecurityEventSubscription {

    void unsubscribe();
}
