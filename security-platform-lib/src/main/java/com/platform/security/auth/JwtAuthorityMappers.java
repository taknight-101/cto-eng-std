package com.platform.security.auth;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Built-in strategies for deriving Spring Security authorities from JWT claims. */
public final class JwtAuthorityMappers {

    private JwtAuthorityMappers() {
    }

    public static Function<JwtClaimsSet, Set<String>> fromClaim(String claimName, String authorityPrefix) {
        return claims -> claims.claimAsStringList(claimName).stream()
                .map(role -> role.startsWith(authorityPrefix) ? role : authorityPrefix + role)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Function<JwtClaimsSet, Set<String>> none() {
        return claims -> Set.of();
    }
}
