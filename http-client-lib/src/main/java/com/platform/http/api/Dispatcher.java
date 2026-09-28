package com.platform.http.api;

import java.util.function.Function;

/**
 * "Give me a resource, dispatch it, and consume the result." Generic, composable functional
 * interface for turning a request-shaped value into a {@link DispatchResult}.
 */
@FunctionalInterface
public interface Dispatcher<R, T> {

    DispatchResult<T> dispatch(R request);

    default <U> Dispatcher<R, U> map(Function<T, U> mapper) {
        return request -> dispatch(request).map(mapper);
    }

    default <U> Dispatcher<R, U> flatMap(Function<T, DispatchResult<U>> mapper) {
        return request -> dispatch(request).flatMap(mapper);
    }
}
