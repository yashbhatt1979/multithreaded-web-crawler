package com.crawler.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Service
public class UrlCanonicalizer {

    private static final Set<String> TRACKING_PARAMETERS =
            new HashSet<>();

    static {
        TRACKING_PARAMETERS.add("utm_source");
        TRACKING_PARAMETERS.add("utm_medium");
        TRACKING_PARAMETERS.add("utm_campaign");
        TRACKING_PARAMETERS.add("utm_term");
        TRACKING_PARAMETERS.add("utm_content");
        TRACKING_PARAMETERS.add("fbclid");
        TRACKING_PARAMETERS.add("gclid");
    }

    public String canonicalize(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        try {

            URI uri = new URI(rawUrl.trim());

            String scheme = uri.getScheme();
            String host = uri.getHost();

            // Only HTTP and HTTPS URLs are allowed
            if (scheme == null || host == null) {
                return null;
            }

            scheme = scheme.toLowerCase();
            host = host.toLowerCase();

            if (!scheme.equals("http")
                    && !scheme.equals("https")) {

                return null;
            }

            // Remove default ports
            int port = uri.getPort();

            if ((scheme.equals("http") && port == 80)
                    || (scheme.equals("https") && port == 443)) {

                port = -1;
            }

            // Preserve the original path structure
            String path = uri.getPath();

            if (path == null || path.isBlank()) {
                path = "/";
            }

            // Remove tracking parameters while preserving
            // meaningful query parameters
            String query =
                    removeTrackingParameters(
                            uri.getRawQuery()
                    );

            /*
             * Fragment is intentionally removed because
             * fragments are handled client-side and are not
             * normally sent to the server.
             */
            return new URI(
                    scheme,
                    null,
                    host,
                    port,
                    path,
                    query,
                    null
            ).toString();

        } catch (URISyntaxException e) {

            return null;
        }
    }

    private String removeTrackingParameters(
            String query
    ) {

        if (query == null || query.isBlank()) {
            return null;
        }

        StringBuilder cleanedQuery =
                new StringBuilder();

        String[] parameters =
                query.split("&");

        for (String parameter : parameters) {

            if (parameter.isBlank()) {
                continue;
            }

            String parameterName = parameter;

            int equalsIndex =
                    parameter.indexOf('=');

            if (equalsIndex >= 0) {

                parameterName =
                        parameter.substring(
                                0,
                                equalsIndex
                        );
            }

            if (TRACKING_PARAMETERS.contains(
                    parameterName.toLowerCase()
            )) {
                continue;
            }

            if (cleanedQuery.length() > 0) {
                cleanedQuery.append("&");
            }

            cleanedQuery.append(parameter);
        }

        return cleanedQuery.length() == 0
                ? null
                : cleanedQuery.toString();
    }
}