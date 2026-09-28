/**
 * Single-dependency distribution artifact: consumers depend on {@code common-lib} and get both
 * {@code security-platform-lib} and {@code http-client-lib} transitively. This module
 * intentionally contains no classes of its own beyond this package - see the root README for
 * why this is an aggregator dependency rather than a shaded uber-jar.
 */
package com.platform.common;
