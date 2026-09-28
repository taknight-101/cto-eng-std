package com.platform.security.auth;

import com.platform.security.api.InvalidTokenException;
import com.platform.security.api.SecurityErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Instant;
import java.util.Map;

/**
 * Default {@link JwtValidator}: delegates entirely to a Spring Security {@link JwtDecoder}
 * (typically backed by {@code NimbusJwtDecoder}, i.e. Nimbus JOSE+JWT) for signature
 * verification and standard claim validation, then adapts the result into {@link JwtClaimsSet}.
 */
public final class DelegatingJwtValidator implements JwtValidator {

    private final JwtDecoder decoder;

    public DelegatingJwtValidator(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public JwtClaimsSet validate(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtValidationException e) {
            SecurityErrorCode code = isExpired(token) ? SecurityErrorCode.TOKEN_EXPIRED : SecurityErrorCode.INVALID_TOKEN;
            throw new InvalidTokenException("JWT failed validation", e, code, Map.of());
        } catch (JwtException e) {
            throw new InvalidTokenException("JWT could not be decoded", e, SecurityErrorCode.INVALID_TOKEN, Map.of());
        }
        return new JwtClaimsSet(
                jwt.getSubject(),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                jwt.getAudience(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                jwt.getClaims());
    }

    /**
     * Distinguishes "expired" from "otherwise invalid" without parsing English error
     * descriptions: re-reads the (already signature-verified-or-not) claims directly and
     * compares the {@code exp} claim to now. Used only for event/error classification, not for
     * trust decisions - {@link #decoder} already rejected the token either way.
     */
    private static boolean isExpired(String token) {
        try {
            var claims = com.nimbusds.jwt.JWTParser.parse(token).getJWTClaimsSet();
            return claims.getExpirationTime() != null && claims.getExpirationTime().toInstant().isBefore(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}
