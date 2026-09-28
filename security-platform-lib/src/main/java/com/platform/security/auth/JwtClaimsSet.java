package com.platform.security.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Library-owned view of validated JWT claims, independent of the underlying decoder implementation. */
public record JwtClaimsSet(
        String subject, String issuer, List<String> audience, Instant issuedAt, Instant expiresAt,
        Map<String, Object> claims) {

    public JwtClaimsSet {
        audience = audience == null ? List.of() : List.copyOf(audience);
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }

    @SuppressWarnings("unchecked")
    public List<String> claimAsStringList(String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String s) {
            return List.of(s.split("\\s+"));
        }
        return List.of();
    }
}
