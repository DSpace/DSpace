/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link org.springframework.boot.actuate.health.HealthIndicator} that verifies if the SEO of the
 * DSpace instance is configured correctly.
 *
 * This is only relevant in a production environment, where the DSpace instance is exposed to the public.
 */
public class SEOHealthIndicator extends AbstractHealthIndicator {

    private static final Pattern CSR_PATTERN = Pattern.compile("<ds-app[^>]*>\\s*</ds-app>");

    @Autowired
    ConfigurationService configurationService;

    RestTemplate restTemplate;

    public SEOHealthIndicator() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String baseUrl = configurationService.getProperty("dspace.ui.url");

        boolean sitemapOk = checkUrl(baseUrl + "/sitemap_index.xml") || checkUrl(baseUrl + "/sitemap_index.html");
        RobotsTxtStatus robotsTxtStatus = checkRobotsTxt(baseUrl + "/robots.txt");
        SsrStatus ssrStatus = checkSSR(baseUrl);

        if (sitemapOk && robotsTxtStatus == RobotsTxtStatus.VALID && ssrStatus == SsrStatus.ENABLED) {
            builder.up()
                   .withDetail("sitemap", "OK")
                   .withDetail("robots.txt", "OK")
                   .withDetail("ssr", "OK");
        } else {
            builder.down();
            builder.withDetail("sitemap", sitemapOk ? "OK" : "Sitemaps are missing or inaccessible. Please see the " +
                    "DSpace Documentation on Search Engine Optimization for how to enable Sitemaps.");

            if (robotsTxtStatus == RobotsTxtStatus.UNKNOWN) {
                builder.withDetail("robots.txt", "Could not be fetched or evaluated. Please check that " +
                        "the DSpace UI is reachable from the backend server.");
            } else if (robotsTxtStatus == RobotsTxtStatus.INVALID) {
                builder.withDetail("robots.txt", "Invalid because it contains localhost URLs. This is often a sign " +
                        "that a proxy is failing to pass X-Forwarded headers to DSpace. Please see the DSpace " +
                        "Documentation on Search Engine Optimization for how to pass X-Forwarded headers.");
            } else {
                builder.withDetail("robots.txt", "OK");
            }

            if (ssrStatus == SsrStatus.DISABLED) {
                builder.withDetail("ssr", "Server-side rendering (SSR) appears to be disabled.  Most " +
                        "search engines require enabling SSR for proper indexing. Please see the DSpace Documentation" +
                        " on Search Engine Optimization for more details.");
            } else if (ssrStatus == SsrStatus.UNKNOWN) {
                builder.withDetail("ssr", "Could not be fetched or evaluated. Please check that " +
                        "the DSpace UI is reachable from the backend server.");
            } else {
                builder.withDetail("ssr", "OK");
            }
        }
    }

    private boolean checkUrl(String url) {
        try {
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private RobotsTxtStatus checkRobotsTxt(String url) {
        try {
            String content = restTemplate.getForObject(url, String.class);
            if (StringUtils.isBlank(content)) {
                return RobotsTxtStatus.UNKNOWN;
            }
            if (content.contains("localhost")) {
                return RobotsTxtStatus.INVALID;
            }
            return RobotsTxtStatus.VALID;
        } catch (Exception e) {
            return RobotsTxtStatus.UNKNOWN;
        }
    }

    private SsrStatus checkSSR(String url) {
        try {
            String content = restTemplate.getForObject(url, String.class);
            if (StringUtils.isBlank(content)) {
                return SsrStatus.UNKNOWN;
            }
            return CSR_PATTERN.matcher(content).find() ? SsrStatus.DISABLED : SsrStatus.ENABLED;
        } catch (Exception e) {
            return SsrStatus.UNKNOWN;
        }
    }

    private enum RobotsTxtStatus {
        VALID, INVALID, UNKNOWN
    }

    private enum SsrStatus {
        ENABLED, DISABLED, UNKNOWN
    }
}
