package com.platform.http.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import com.platform.http.api.HttpRequestSpec;

/** Thread-safe {@link HttpMiddlewareRegistry}; reads are lock-free via a volatile snapshot. */
public final class DefaultHttpMiddlewareRegistry implements HttpMiddlewareRegistry {

    private static final int DEFAULT_ORDER_SPACING = 100;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong versionCounter = new AtomicLong();
    private volatile List<HttpMiddlewareRegistration> snapshot = List.of();

    @Override
    public HttpMiddlewareRegistry register(HttpMiddleware middleware) {
        return register(middleware.name(), middleware);
    }

    @Override
    public HttpMiddlewareRegistry register(String name, HttpMiddleware middleware) {
        lock.lock();
        try {
            int nextOrder = snapshot.isEmpty() ? DEFAULT_ORDER_SPACING
                    : snapshot.get(snapshot.size() - 1).order() + DEFAULT_ORDER_SPACING;
            return register(name, middleware, nextOrder);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public HttpMiddlewareRegistry register(String name, HttpMiddleware middleware, int order) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(middleware, "middleware must not be null");
        mutate(current -> {
            if (indexOf(current, name) >= 0) {
                throw new IllegalStateException("Middleware '" + name + "' is already registered; use replace(...) instead");
            }
            List<HttpMiddlewareRegistration> updated = new ArrayList<>(current);
            HttpMiddlewareRegistration registration = new HttpMiddlewareRegistration(name, middleware, order, true, null);
            int insertAt = 0;
            while (insertAt < updated.size() && updated.get(insertAt).order() <= order) {
                insertAt++;
            }
            updated.add(insertAt, registration);
            return updated;
        });
        return this;
    }

    @Override
    public HttpMiddlewareRegistry registerBefore(String existingName, String name, HttpMiddleware middleware) {
        mutate(current -> insertRelative(current, existingName, name, middleware, -1));
        return this;
    }

    @Override
    public HttpMiddlewareRegistry registerAfter(String existingName, String name, HttpMiddleware middleware) {
        mutate(current -> insertRelative(current, existingName, name, middleware, +1));
        return this;
    }

    @Override
    public HttpMiddlewareRegistry replace(String name, HttpMiddleware middleware) {
        mutate(current -> updateExisting(current, name, reg -> reg.withMiddleware(middleware)));
        return this;
    }

    @Override
    public HttpMiddlewareRegistry remove(String name) {
        mutate(current -> {
            List<HttpMiddlewareRegistration> updated = new ArrayList<>(current);
            updated.removeIf(reg -> reg.name().equals(name));
            return updated;
        });
        return this;
    }

    @Override
    public HttpMiddlewareRegistry enable(String name) {
        mutate(current -> updateExisting(current, name, reg -> reg.withEnabled(true)));
        return this;
    }

    @Override
    public HttpMiddlewareRegistry disable(String name) {
        mutate(current -> updateExisting(current, name, reg -> reg.withEnabled(false)));
        return this;
    }

    @Override
    public HttpMiddlewareRegistry condition(String name, Predicate<HttpRequestSpec> condition) {
        mutate(current -> updateExisting(current, name, reg -> reg.withCondition(condition)));
        return this;
    }

    @Override
    public List<HttpMiddlewareRegistration> registrations() {
        return snapshot;
    }

    @Override
    public long version() {
        return versionCounter.get();
    }

    private void mutate(UnaryOperator<List<HttpMiddlewareRegistration>> mutation) {
        lock.lock();
        try {
            snapshot = List.copyOf(mutation.apply(snapshot));
            versionCounter.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    private static List<HttpMiddlewareRegistration> insertRelative(
            List<HttpMiddlewareRegistration> current, String existingName, String name, HttpMiddleware middleware, int direction) {
        int existingIndex = indexOf(current, existingName);
        if (existingIndex < 0) {
            throw new NoSuchElementException("No middleware registered with name '" + existingName + "'");
        }
        if (indexOf(current, name) >= 0) {
            throw new IllegalStateException("Middleware '" + name + "' is already registered; use replace(...) instead");
        }
        HttpMiddlewareRegistration anchor = current.get(existingIndex);
        int order = direction < 0 ? anchor.order() - 1 : anchor.order() + 1;
        List<HttpMiddlewareRegistration> updated = new ArrayList<>(current);
        int insertAt = direction < 0 ? existingIndex : existingIndex + 1;
        updated.add(insertAt, new HttpMiddlewareRegistration(name, middleware, order, true, null));
        return updated;
    }

    private static List<HttpMiddlewareRegistration> updateExisting(
            List<HttpMiddlewareRegistration> current, String name, UnaryOperator<HttpMiddlewareRegistration> update) {
        int index = indexOf(current, name);
        if (index < 0) {
            throw new NoSuchElementException("No middleware registered with name '" + name + "'");
        }
        List<HttpMiddlewareRegistration> updated = new ArrayList<>(current);
        updated.set(index, update.apply(updated.get(index)));
        return updated;
    }

    private static int indexOf(List<HttpMiddlewareRegistration> registrations, String name) {
        for (int i = 0; i < registrations.size(); i++) {
            if (registrations.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
