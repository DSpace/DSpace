/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.dspace.app.rest.configuration.ActuatorConfiguration;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link SEOHealthIndicator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SEOHealthIndicatorTest {

    private static final String BASE_URL = "https://demo.dspace.org";

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SEOHealthIndicator seoHealthIndicator;

    @Before
    public void setUp() {
        when(configurationService.getProperty("dspace.ui.url")).thenReturn(BASE_URL);
    }

    @Test
    public void testWithSeoConfiguredCorrectly() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<sitemap/>"));
        when(restTemplate.getForObject(eq(BASE_URL + "/robots.txt"), eq(String.class)))
            .thenReturn("User-agent: *\nSitemap: " + BASE_URL + "/sitemap_index.xml");
        when(restTemplate.getForObject(eq(BASE_URL), eq(String.class)))
            .thenReturn("<html><body>Rendered content</body></html>");

        Health health = seoHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.UP));
        assertThat(health.getDetails(), is(Map.of(
            SEOHealthIndicator.SITEMAP, SEOHealthIndicator.OK,
            SEOHealthIndicator.ROBOTS_TXT, SEOHealthIndicator.OK,
            SEOHealthIndicator.SSR, SEOHealthIndicator.OK)));
    }

    @Test
    public void testWithSeoChecksFailingReportsUpWithIssues() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("connection refused"));
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("connection refused"));

        Health health = seoHealthIndicator.health();

        assertThat(health.getStatus(), is(ActuatorConfiguration.UP_WITH_ISSUES_STATUS));
        assertThat(health.getDetails(), is(Map.of(
            SEOHealthIndicator.SITEMAP, SEOHealthIndicator.SITEMAP_MISSING,
            SEOHealthIndicator.ROBOTS_TXT, SEOHealthIndicator.ROBOTS_MISSING,
            SEOHealthIndicator.SSR, SEOHealthIndicator.SSR_DISABLED)));
    }

    @Test
    public void testWithRobotsTxtContainingLocalhostReportsUpWithIssues() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<sitemap/>"));
        when(restTemplate.getForObject(eq(BASE_URL + "/robots.txt"), eq(String.class)))
            .thenReturn("Sitemap: http://localhost:8080/sitemap_index.xml");
        when(restTemplate.getForObject(eq(BASE_URL), eq(String.class)))
            .thenReturn("<html><body>Rendered content</body></html>");

        Health health = seoHealthIndicator.health();

        assertThat(health.getStatus(), is(ActuatorConfiguration.UP_WITH_ISSUES_STATUS));
        assertThat(health.getDetails(), is(Map.of(
            SEOHealthIndicator.SITEMAP, SEOHealthIndicator.OK,
            SEOHealthIndicator.ROBOTS_TXT, SEOHealthIndicator.ROBOTS_INVALID,
            SEOHealthIndicator.SSR, SEOHealthIndicator.OK)));
    }

    @Test
    public void testWithSsrDisabledReportsUpWithIssues() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("<sitemap/>"));
        when(restTemplate.getForObject(eq(BASE_URL + "/robots.txt"), eq(String.class)))
            .thenReturn("User-agent: *\nSitemap: " + BASE_URL + "/sitemap_index.xml");
        when(restTemplate.getForObject(eq(BASE_URL), eq(String.class)))
            .thenReturn("<ds-app></ds-app>");

        Health health = seoHealthIndicator.health();

        assertThat(health.getStatus(), is(ActuatorConfiguration.UP_WITH_ISSUES_STATUS));
        assertThat(health.getDetails(), is(Map.of(
            SEOHealthIndicator.SITEMAP, SEOHealthIndicator.OK,
            SEOHealthIndicator.ROBOTS_TXT, SEOHealthIndicator.OK,
            SEOHealthIndicator.SSR, SEOHealthIndicator.SSR_DISABLED)));
    }
}
