/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Global filter acting on all requests (not just /api/) to provide some additional hardening
 * against common attacks or RCE, if a malicious payload was somehow written to a directory
 * executable by the servlet container.
 * The decoding and normalisation is designed to be tolerant of malformed URLs or broken clients, etc.
 * so that this additional security filter does not introduce false positives or unintended side effects.
 *
 * @author Kim Shepherd
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalRequestSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String normalizedPath = normaliseUrl(request.getRequestURI());
        // Return 403 forbidden if JSP execution or URL traversal is attempted
        if (isTraversalAttempt(normalizedPath)) {
            logger.warn("Path traversal attempt detected. Skipping request: " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (isJspExecutionAttempt(normalizedPath)) {
            logger.warn("JSP execution attempt detected. Skipping request: " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Normalise the URI similarly to Tomcat, for testing how it will be interpreted
     * @param rawUrl the unvalidated URL string
     * @return a decoded, normalise URL
     */
    private String normaliseUrl(String rawUrl) throws IOException {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IOException("Empty URL");
        }
        String url = rawUrl.split("\\?")[0];
        // Strip ;jspsession=... and so on
        int semicolon = url.indexOf(';');
        if (semicolon >= 0) {
            url = url.substring(0, semicolon);
        }
        url = decodeUrl(url);
        if (url == null || url.isBlank()) {
            throw new IOException("Decoded URL path is empty");
        }
        url = normaliseUrlPath(url);
        if (url == null || url.isBlank()) {
            throw new IOException("Normalised URL path is empty");
        }
        return url.toLowerCase(Locale.ROOT);
    }

    /**
     * Decode URL, falling back to original URL if it's malformed or undecodable
     * @param url the encoded / unvalidated URL
     * @return decoded URL or the original URL on error
     */
    private String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // if we can't decode it, just return raw string
            return url;
        }
    }

    /**
     * Normalise the URL path and ensure it ends in a /
     * @param url the URL path to normalise
     * @return normalised path or the original parameter on error
     */
    private String normaliseUrlPath(String url) {
        try {
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            return new URI(url).normalize().getPath();
        } catch (Exception e) {
            // if we can't use or normalise the path, just return the raw string
            return url;
        }
    }

    /**
     * Detect traversal after normalisation
     * @param url the URL path to validate
     * @return true if this looks like a traversal attempt
     */
    private boolean isTraversalAttempt(String url) {
        return url.contains("../")
                || url.contains("/..")
                || url.contains("%2e%2e")
                || url.contains("..");
    }

    /**
     * Block JSP execution attempts
     * @param url the URL path to validate
     */
    private boolean isJspExecutionAttempt(String url) {
        return url.endsWith(".jsp")
                || url.endsWith(".jspx")
                || url.contains(".jsp/")
                || url.contains(".jspx/")
                || url.contains(".jsp\0")
                || url.contains(".jspx\0");
    }
}
