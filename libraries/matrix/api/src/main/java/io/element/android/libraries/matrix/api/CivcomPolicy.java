/*
 * Copyright (c) 2026 KG PSP.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.libraries.matrix.api;

import java.net.URI;

/** Immutable deployment boundary shared by login and excluded services. */
public final class CivcomPolicy {
    public static final String SERVER_NAME = "soia.info";
    public static final String HOMESERVER = "https://matrix.soia.info";
    public static final String AUTH_ISSUER = "https://auth.soia.info/";
    public static final boolean CALLS_ENABLED = false;
    public static final boolean MAPS_ENABLED = false;

    private CivcomPolicy() { }

    public static boolean isAllowedHomeserver(String value) {
        if (SERVER_NAME.equals(value)) return true;
        try {
            URI uri = URI.create(value);
            return isHttps(uri) && (SERVER_NAME.equals(uri.getHost()) || "matrix.soia.info".equals(uri.getHost()))
                    && (uri.getPath().isEmpty() || "/".equals(uri.getPath())) && uri.getQuery() == null;
        } catch (RuntimeException error) {
            return false;
        }
    }

    public static boolean isTrustedOAuthEndpoint(String value) {
        try {
            URI uri = URI.create(value);
            return isHttps(uri) && "auth.soia.info".equals(uri.getHost());
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme()) && uri.getUserInfo() == null && uri.getFragment() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
    }
}
