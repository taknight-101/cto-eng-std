/**
 * Public API of the security platform: {@link com.platform.security.api.SecurityRequestContext},
 * the middleware pipeline, authentication/authorization contracts and the security event drill.
 * Implementations live in sibling packages ({@code context}, {@code pipeline}, {@code auth},
 * {@code authorization}, {@code events}, {@code spring}); this package should be the only one
 * most application code needs to import directly.
 */
package com.platform.security.api;
