package com.platform.security.spi;

import com.platform.security.api.SecurityRequestContext;

/**
 * User-implementable extension point for cheap, early accept/reject checks (e.g. an IP
 * allowlist) that don't need full {@link com.platform.security.api.SecurityMiddleware} chain
 * control. Register via an adapting middleware if you need this to run inside the pipeline.
 */
@FunctionalInterface
public interface SecurityRequestInspector {

    InspectionResult inspect(SecurityRequestContext context);

    sealed interface InspectionResult {
        record Accept() implements InspectionResult {
        }

        record Reject(String reason) implements InspectionResult {
        }

        static InspectionResult accept() {
            return new Accept();
        }

        static InspectionResult reject(String reason) {
            return new Reject(reason);
        }
    }
}
