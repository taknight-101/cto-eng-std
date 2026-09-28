package com.platform.http.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Generic type token for typed responses that need a parameterized type, e.g.
 * {@code client.execute(request, new TypeRef<List<User>>() {})}.
 */
public abstract class TypeRef<T> {

    private final Type type;

    protected TypeRef() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("TypeRef must be created as an anonymous subclass, e.g. new TypeRef<List<User>>(){}");
        }
        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    public Type type() {
        return type;
    }
}
