/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String baseUrl = configurationService.getProperty("dspace.ui.url");

        boolean sitemapOk = checkUrl(baseUrl + "/sitemap_index.xml") || checkUrl(baseUrl + "/sitemap_index.html");
        boolean robotsTxtOk = checkRobotsTxt(baseUrl + "/robots.txt");
        boolean ssrOk = checkSSR(baseUrl);

        if (sitemapOk && robotsTxtOk && ssrOk) {
            builder.up()
                   .withDetail("sitemap", "OK")
                   .withDetail("robots.txt", "OK")
                   .withDetail("ssr", "OK");
        } else {
            builder.down()
                   .withDetail("sitemap", sitemapOk ? "OK" : "Missing or inaccessible")
                   .withDetail("robots.txt", robotsTxtOk ? "OK" : "Empty or contains local URLs")
                   .withDetail("ssr", ssrOk ? "OK" : "Server-side rendering might be disabled");
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

    private boolean checkRobotsTxt(String url) {
        try {
            String content = restTemplate.getForObject(url, String.class);
            return StringUtils.isNotBlank(content) && !content.contains("localhost");
        } catch (Exception e) {
            return false;
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
}

