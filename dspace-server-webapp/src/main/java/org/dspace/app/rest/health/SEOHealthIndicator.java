/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.dspace.app.rest.configuration.ActuatorConfiguration.UP_WITH_ISSUES_STATUS;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link org.springframework.boot.actuate.health.HealthIndicator} that verifies if the SEO of the
 * DSpace instance is configured correctly.
 *
 * This is only relevant in a production environment, where the DSpace instance is exposed to the public.
 */
public class SEOHealthIndicator extends AbstractHealthIndicator {

    public static final String SITEMAP = "sitemap";
    public static final String ROBOTS_TXT = "robots.txt";
    public static final String SSR = "ssr";
    public static final String OK = "OK";
    public static final String SITEMAP_MISSING = "Sitemaps are missing or inaccessible. Please see the " +
        "DSpace Documentation on Search Engine Optimization for how to enable " +
        "Sitemaps.";
    public static final String ROBOTS_MISSING = "Missing or inaccessible. Please see the DSpace Documentation on " +
        "Search Engine Optimization for how to create a robots.txt.";
    public static final String ROBOTS_INVALID = "Invalid because it contains localhost URLs. This is often a sign " +
        "that a proxy is failing to pass X-Forwarded headers to DSpace. Please see the DSpace " +
        "Documentation on Search Engine Optimization for how to pass X-Forwarded headers.";
    public static final String SSR_DISABLED = "Server-side rendering (SSR) appears to be disabled.  Most " +
        "search engines require enabling SSR for proper indexing. Please see the DSpace Documentation on" +
        " Search Engine Optimization for more details.";

    @Autowired
    private ConfigurationService configurationService;

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String baseUrl = configurationService.getProperty("dspace.ui.url");

        boolean sitemapOk = checkUrl(baseUrl + "/sitemap_index.xml") || checkUrl(baseUrl + "/sitemap_index.html");
        RobotsTxtStatus robotsTxtStatus = checkRobotsTxt(baseUrl + "/robots.txt");
        boolean ssrOk = checkSSR(baseUrl);

        if (sitemapOk && robotsTxtStatus == RobotsTxtStatus.VALID && ssrOk) {
            builder.up()
                   .withDetail(SITEMAP, OK)
                   .withDetail(ROBOTS_TXT, OK)
                   .withDetail(SSR, OK);
        } else {
            builder.status(UP_WITH_ISSUES_STATUS)
                   .withDetail(SITEMAP, sitemapOk ? OK : SITEMAP_MISSING);

            if (robotsTxtStatus == RobotsTxtStatus.MISSING) {
                builder.withDetail(ROBOTS_TXT, ROBOTS_MISSING);
            } else if (robotsTxtStatus == RobotsTxtStatus.INVALID) {
                builder.withDetail(ROBOTS_TXT, ROBOTS_INVALID);
            } else {
                builder.withDetail(ROBOTS_TXT, OK);
            }
            builder.withDetail(SSR, ssrOk ? OK : SSR_DISABLED);
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
                return RobotsTxtStatus.MISSING;
            }
            if (content.contains("localhost")) {
                return RobotsTxtStatus.INVALID;
            }
            return RobotsTxtStatus.VALID;
        } catch (Exception e) {
            return RobotsTxtStatus.MISSING;
        }
    }

    private boolean checkSSR(String url) {
        try {
            String content = restTemplate.getForObject(url, String.class);
            return content != null && !content.contains("<ds-app></ds-app>");
        } catch (Exception e) {
            return false;
        }
    }

    private enum RobotsTxtStatus {
        VALID, MISSING, INVALID
    }
}

