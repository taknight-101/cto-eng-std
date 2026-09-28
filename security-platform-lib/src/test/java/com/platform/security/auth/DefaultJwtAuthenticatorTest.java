package com.platform.security.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.platform.security.api.AuthenticationOutcome;
import com.platform.security.api.SecurityErrorCode;
import com.platform.security.api.SecurityRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJwtAuthenticatorTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";

    @Test
    void validTokenAuthenticatesSuccessfully() throws Exception {
        String token = signedToken(claims("alice", Instant.now().minusSeconds(10), Instant.now().plusSeconds(300), List.of("ADMIN")));
        DefaultJwtAuthenticator authenticator = authenticator();
        SecurityRequestContext context = contextWithBearer(token);

        AuthenticationOutcome outcome = authenticator.authenticate(context);

        assertThat(outcome).isInstanceOfSatisfying(AuthenticationOutcome.Success.class, success -> {
            JwtAuthenticationToken token1 = (JwtAuthenticationToken) success.authentication();
            assertThat(token1.claims().subject()).isEqualTo("alice");
            assertThat(token1.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
        });
    }

    @Test
    void expiredTokenFailsWithTokenExpiredErrorCode() throws Exception {
        String token = signedToken(claims("alice", Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS), List.of()));
        AuthenticationOutcome outcome = authenticator().authenticate(contextWithBearer(token));

        assertThat(outcome).isInstanceOfSatisfying(AuthenticationOutcome.Failure.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(SecurityErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void malformedTokenFailsWithInvalidTokenErrorCode() {
        AuthenticationOutcome outcome = authenticator().authenticate(contextWithBearer("not-a-jwt"));

        assertThat(outcome).isInstanceOfSatisfying(AuthenticationOutcome.Failure.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(SecurityErrorCode.INVALID_TOKEN));
    }

    @Test
    void missingTokenIsNotApplicable() {
        SecurityRequestContext context = SecurityRequestContext.builder().build();
        AuthenticationOutcome outcome = authenticator().authenticate(context);

        assertThat(outcome).isInstanceOf(AuthenticationOutcome.NotApplicable.class);
    }

    private DefaultJwtAuthenticator authenticator() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return new DefaultJwtAuthenticator(
                BearerTokenExtractor.authorizationHeader(),
                new DelegatingJwtValidator(decoder),
                JwtAuthorityMappers.fromClaim("roles", "ROLE_"));
    }

    private static SecurityRequestContext contextWithBearer(String token) {
        return SecurityRequestContext.builder().header("Authorization", "Bearer " + token).build();
    }

    private static JWTClaimsSet claims(String subject, Instant issuedAt, Instant expiry, List<String> roles) {
        return new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiry))
                .claim("roles", roles)
                .build();
    }

    private static String signedToken(JWTClaimsSet claims) throws Exception {
        JWSSigner signer = new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8));
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
