package com.platform.security.api;

/** Represents the remainder of the {@link SecurityPipeline} from the current middleware's viewpoint. */
@FunctionalInterface
public interface SecurityMiddlewareChain {

    SecurityMiddlewareOutcome proceed(SecurityRequestContext context) throws Exception;
}
