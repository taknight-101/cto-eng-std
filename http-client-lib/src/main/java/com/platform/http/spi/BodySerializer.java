package com.platform.http.spi;

/** Pluggable request body serialization, defaulting to a Jackson-backed implementation. */
@FunctionalInterface
public interface BodySerializer {

    byte[] serialize(Object value, String contentType);
}
