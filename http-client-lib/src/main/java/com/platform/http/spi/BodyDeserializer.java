package com.platform.http.spi;

import java.lang.reflect.Type;

/** Pluggable response body deserialization, defaulting to a Jackson-backed implementation. */
public interface BodyDeserializer {

    <T> T deserialize(byte[] bytes, Type type, String contentType);
}
