package com.platform.example.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * DEV-ONLY: mints a demo HS256 JWT signed with the same symmetric secret the app validates
 * with, so this example is runnable without a real identity provider. A real application would
 * never issue its own tokens like this outside of tests - use a proper IdP / OAuth2
 * authorization server, and asymmetric (RS256/ES256) keys via {@code jwk-set-uri} in production.
 *
 * <p>Disabled under the {@code docker} profile, where JWTs come from a real Keycloak instead
 * (see {@code infra/README.md}) - this endpoint would otherwise mint tokens that fail
 * validation against Keycloak's JWKS, which is confusing rather than useful.
 */
@RestController
@RequestMapping("/dev")
@Profile("!docker")
public class DevTokenController {

    private final String hmacSecret;

    public DevTokenController(@Value("${security-platform.jwt.hmac-secret}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    @GetMapping("/token")
    public Map<String, String> token(
            @RequestParam String subject, @RequestParam(defaultValue = "USER") String roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .claim("roles", Arrays.asList(roles.split(",")))
                .build();
        JWSSigner signer = new MACSigner(hmacSecret.getBytes(StandardCharsets.UTF_8));
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(signer);
        return Map.of("token", signedJwt.serialize());
    }
}
