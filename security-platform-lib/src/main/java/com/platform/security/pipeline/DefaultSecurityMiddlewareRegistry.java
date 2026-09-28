package com.platform.security.pipeline;

import com.platform.security.api.SecurityMiddleware;
import com.platform.security.api.SecurityMiddlewareRegistration;
import com.platform.security.api.SecurityMiddlewareRegistry;
import com.platform.security.api.SecurityRequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Thread-safe {@link SecurityMiddlewareRegistry}. Reads ({@link #registrations()}) are
 * lock-free, served from a volatile immutable snapshot; mutations are serialized with a lock
 * and publish a new snapshot plus a bumped {@link #version()}.
 */
public final class DefaultSecurityMiddlewareRegistry implements SecurityMiddlewareRegistry {

    private static final int DEFAULT_ORDER_SPACING = 100;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong versionCounter = new AtomicLong();
    private volatile List<SecurityMiddlewareRegistration> snapshot = List.of();

    @Override
    public SecurityMiddlewareRegistry register(SecurityMiddleware middleware) {
        return register(middleware.name(), middleware);
    }

    @Override
    public SecurityMiddlewareRegistry register(String name, SecurityMiddleware middleware) {
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
    public SecurityMiddlewareRegistry register(String name, SecurityMiddleware middleware, int order) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(middleware, "middleware must not be null");
        mutate(current -> {
            if (indexOf(current, name) >= 0) {
                throw new IllegalStateException(
                        "Middleware '" + name + "' is already registered; use replace(...) instead");
            }
            List<SecurityMiddlewareRegistration> updated = new ArrayList<>(current);
            SecurityMiddlewareRegistration registration =
                    new SecurityMiddlewareRegistration(name, middleware, order, true, null);
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
    public SecurityMiddlewareRegistry registerBefore(String existingName, String name, SecurityMiddleware middleware) {
        mutate(current -> insertRelative(current, existingName, name, middleware, -1));
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry registerAfter(String existingName, String name, SecurityMiddleware middleware) {
        mutate(current -> insertRelative(current, existingName, name, middleware, +1));
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry replace(String name, SecurityMiddleware middleware) {
        mutate(current -> updateExisting(current, name, reg -> reg.withMiddleware(middleware)));
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry remove(String name) {
        mutate(current -> {
            List<SecurityMiddlewareRegistration> updated = new ArrayList<>(current);
            updated.removeIf(reg -> reg.name().equals(name));
            return updated;
        });
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry enable(String name) {
        mutate(current -> updateExisting(current, name, reg -> reg.withEnabled(true)));
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry disable(String name) {
        mutate(current -> updateExisting(current, name, reg -> reg.withEnabled(false)));
        return this;
    }

    @Override
    public SecurityMiddlewareRegistry condition(String name, Predicate<SecurityRequestContext> condition) {
        mutate(current -> updateExisting(current, name, reg -> reg.withCondition(condition)));
        return this;
    }

    @Override
    public List<SecurityMiddlewareRegistration> registrations() {
        return snapshot;
    }

    @Override
    public long version() {
        return versionCounter.get();
    }

    private void mutate(UnaryOperator<List<SecurityMiddlewareRegistration>> mutation) {
        lock.lock();
        try {
            snapshot = List.copyOf(mutation.apply(snapshot));
            versionCounter.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    private static List<SecurityMiddlewareRegistration> insertRelative(
            List<SecurityMiddlewareRegistration> current,
            String existingName,
            String name,
            SecurityMiddleware middleware,
            int direction) {
        int existingIndex = indexOf(current, existingName);
        if (existingIndex < 0) {
            throw new NoSuchElementException("No middleware registered with name '" + existingName + "'");
        }
        if (indexOf(current, name) >= 0) {
            throw new IllegalStateException("Middleware '" + name + "' is already registered; use replace(...) instead");
        }
        SecurityMiddlewareRegistration anchor = current.get(existingIndex);
        int order = direction < 0 ? anchor.order() - 1 : anchor.order() + 1;
        List<SecurityMiddlewareRegistration> updated = new ArrayList<>(current);
        int insertAt = direction < 0 ? existingIndex : existingIndex + 1;
        updated.add(insertAt, new SecurityMiddlewareRegistration(name, middleware, order, true, null));
        return updated;
    }

    private static List<SecurityMiddlewareRegistration> updateExisting(
            List<SecurityMiddlewareRegistration> current,
            String name,
            UnaryOperator<SecurityMiddlewareRegistration> update) {
        int index = indexOf(current, name);
        if (index < 0) {
            throw new NoSuchElementException("No middleware registered with name '" + name + "'");
        }
        List<SecurityMiddlewareRegistration> updated = new ArrayList<>(current);
        updated.set(index, update.apply(updated.get(index)));
        return updated;
    }

    private static int indexOf(List<SecurityMiddlewareRegistration> registrations, String name) {
        for (int i = 0; i < registrations.size(); i++) {
            if (registrations.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
