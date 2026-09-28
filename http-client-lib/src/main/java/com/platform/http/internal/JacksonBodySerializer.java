package com.platform.http.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.http.api.RequestConstructionException;
import com.platform.http.spi.BodySerializer;

import java.nio.charset.StandardCharsets;

/** Default {@link BodySerializer}, backed by Jackson. */
public final class JacksonBodySerializer implements BodySerializer {

    private final ObjectMapper objectMapper;

    public JacksonBodySerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value, String contentType) {
        try {
            if (value instanceof String s) {
                return s.getBytes(StandardCharsets.UTF_8);
            }
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new RequestConstructionException("Failed to serialize request body", e);
        }
    }
}
