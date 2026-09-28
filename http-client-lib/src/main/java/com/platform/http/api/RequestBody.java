package com.platform.http.api;

/** Immutable request body variants; deliberately not a raw {@code Object} everywhere so intent is explicit. */
public sealed interface RequestBody {

    static RequestBody empty() {
        return Empty.INSTANCE;
    }

    static RequestBody json(Object value) {
        return new Json(value);
    }

    static RequestBody raw(byte[] bytes, String contentType) {
        return new Raw(bytes.clone(), contentType);
    }

    final class Empty implements RequestBody {
        static final Empty INSTANCE = new Empty();

        private Empty() {
        }
    }

    record Json(Object value) implements RequestBody {
    }

    record Raw(byte[] bytes, String contentType) implements RequestBody {
        public Raw {
            bytes = bytes.clone();
        }

        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
