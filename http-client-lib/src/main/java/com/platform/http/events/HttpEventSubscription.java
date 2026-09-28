package com.platform.http.events;

/** Handle returned by {@link HttpEventDrill#subscribe}, allowing a consumer to detach. */
@FunctionalInterface
public interface HttpEventSubscription {

    void unsubscribe();
}
