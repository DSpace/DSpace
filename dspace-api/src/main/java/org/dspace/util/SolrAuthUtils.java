/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;

/**
 * Shared helper to optionally attach preemptive HTTP Basic authentication
 * to an HttpClientBuilder used for talking to a Solr instance, based on
 * configuration.
 * <p>
 * IMPORTANT: callers must add the returned interceptor directly to their
 * own local {@link HttpClientBuilder} instance -- NEVER register it as a
 * Spring {@code HttpRequestInterceptor} bean. {@link org.dspace.app.client.DSpaceHttpClientFactory}
 * auto-applies every such bean to every client it builds, which would leak
 * these credentials to unrelated destinations (ORCID, Crossref, Google
 * Analytics, etc.).
 */
public final class SolrAuthUtils {
    private static final Logger log = LogManager.getLogger();

    private SolrAuthUtils() { }

    /**
     * If PREFIX.authentication.type=basic, add a preemptive Basic
     * "Authorization" header to every request built from this builder.
     * No-op if the property is unset/blank/"none".
     */
    public static void addAuthenticationIfConfigured(HttpClientBuilder builder, String configPrefix,
                                                       ConfigurationService configurationService) {
        addAuthenticationIfConfigured(builder, configPrefix, null, configurationService);
    }

    /**
     * Same as above, but if PREFIX.authentication.type is not set, falls
     * back to checking FALLBACK_PREFIX.authentication.type instead. Useful
     * for Solr cores/services that, by default configuration, share the
     * same Solr instance (and therefore credentials) as the main
     * ${solr.server} pool, but may optionally be pointed at a separate
     * instance with its own credentials.
     *
     * @param fallbackPrefix e.g. "solr". May be null to disable the fallback.
     */
    public static void addAuthenticationIfConfigured(HttpClientBuilder builder, String configPrefix,
                                                       String fallbackPrefix,
                                                       ConfigurationService configurationService) {
        String headerValue = getAuthorizationHeaderValue(configPrefix, fallbackPrefix, configurationService);
        if (headerValue != null) {
            builder.addInterceptorFirst(new PreemptiveBasicAuthInterceptor(headerValue));
        }
    }

    /**
     * Resolve the "Authorization" header value to use for Solr requests, if
     * PREFIX.authentication.type (or FALLBACK_PREFIX.authentication.type) is
     * set to "basic". Useful for callers that can't use an
     * HttpRequestInterceptor -- e.g. java.net.http.HttpClient -- and need to
     * add the header to each outgoing request themselves.
     *
     * @return the header value (e.g. "Basic abc123..."), or null if
     *         authentication is not configured.
     */
    public static String getAuthorizationHeaderValue(String configPrefix, String fallbackPrefix,
                                                       ConfigurationService configurationService) {
        String prefix = configPrefix;
        String authType = configurationService.getProperty(prefix + ".authentication.type");
        if (StringUtils.isBlank(authType) && StringUtils.isNotBlank(fallbackPrefix)) {
            prefix = fallbackPrefix;
            authType = configurationService.getProperty(prefix + ".authentication.type");
        }

        if (StringUtils.isBlank(authType) || StringUtils.equalsIgnoreCase(authType, "none")) {
            return null;
        }
        if (!StringUtils.equalsIgnoreCase(authType, "basic")) {
            log.warn("Unrecognized value '{}' for {}.authentication.type (only 'basic' is currently "
                    + "supported); no authentication will be added.", authType, prefix);
            return null;
        }

        String user = configurationService.getProperty(prefix + ".authentication.user");
        String password = configurationService.getProperty(prefix + ".authentication.password");
        if (StringUtils.isBlank(user)) {
            log.warn("{}.authentication.type=basic but {}.authentication.user is not set; "
                    + "no authentication will be added.", prefix, prefix);
            return null;
        }

        String token = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static final class PreemptiveBasicAuthInterceptor implements HttpRequestInterceptor {
        private final String headerValue;

        private PreemptiveBasicAuthInterceptor(String headerValue) {
            this.headerValue = headerValue;
        }

        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException {
            if (!request.containsHeader("Authorization")) {
                request.addHeader("Authorization", headerValue);
            }
        }
    }
}