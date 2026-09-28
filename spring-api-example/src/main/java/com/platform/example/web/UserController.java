package com.platform.example.web;

import com.platform.security.api.SecurityContextAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/users/{id}}: demonstrates security context access from application code -
 * by the time this controller runs, JWT authentication and authorization have already happened
 * in {@code SecurityMiddlewareFilter}.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SecurityContextAccessor securityContextAccessor;

    public UserController(SecurityContextAccessor securityContextAccessor) {
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable String id) {
        String requestedBy = securityContextAccessor.currentPrincipal()
                .map(principal -> principal.name())
                .orElse("anonymous");
        return new UserResponse(id, "User " + id, requestedBy);
    }
}
