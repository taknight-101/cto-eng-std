package com.platform.http.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.http.api.ResponseDeserializationException;
import com.platform.http.spi.BodyDeserializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/** Default {@link BodyDeserializer}, backed by Jackson. */
public final class JacksonBodyDeserializer implements BodyDeserializer {

    private final ObjectMapper objectMapper;

    public JacksonBodyDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Type type, String contentType) {
        if (bytes.length == 0) {
            return null;
        }
        try {
            if (type == String.class) {
                return (T) new String(bytes, StandardCharsets.UTF_8);
            }
            if (type == byte[].class) {
                return (T) bytes;
            }
            return objectMapper.readValue(bytes, objectMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new ResponseDeserializationException("Failed to deserialize response body", e);
        }
    }
}
