/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link SEOHealthIndicator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SEOHealthIndicatorTest {

    private static final String BASE_URL = "http://localhost:4000";

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SEOHealthIndicator indicator;

    private void mockBaseUrl() {
        when(configurationService.getProperty("dspace.ui.url")).thenReturn(BASE_URL);
    }

    @Test
    public void testAllOk() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("User-agent: *");
        when(restTemplate.getForObject(BASE_URL, String.class))
            .thenReturn("<html><ds-app ng-version=\"12\">rendered content</ds-app></html>");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.UP));
        assertThat(health.getDetails().get("ssr"), is("OK"));
        assertThat(health.getDetails().get("robots.txt"), is("OK"));
    }

    @Test
    public void testSsrDisabled() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("User-agent: *");
        when(restTemplate.getForObject(BASE_URL, String.class)).thenReturn("<html><ds-app></ds-app></html>");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("ssr"), containsString("disabled"));
    }

    @Test
    public void testSsrDisabledWithAttributes() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("User-agent: *");
        when(restTemplate.getForObject(BASE_URL, String.class))
            .thenReturn("<html><ds-app ng-version=\"12.2.5\"></ds-app></html>");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("ssr"), containsString("disabled"));
    }

    @Test
    public void testSsrUnknownOnException() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("User-agent: *");
        when(restTemplate.getForObject(BASE_URL, String.class))
            .thenThrow(new ResourceAccessException("timeout"));
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("ssr"), containsString("Could not be fetched"));
    }

    @Test
    public void testSsrUnknownOnEmptyBody() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("User-agent: *");
        when(restTemplate.getForObject(BASE_URL, String.class)).thenReturn("");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("ssr"), containsString("Could not be fetched"));
    }

    @Test
    public void testRobotsTxtUnknownOnException() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class))
            .thenThrow(new ResourceAccessException("timeout"));
        when(restTemplate.getForObject(BASE_URL, String.class)).thenReturn("<html>ssr rendered</html>");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("robots.txt"), containsString("Could not be fetched"));
    }

    @Test
    public void testRobotsTxtUnknownOnEmptyBody() {
        mockBaseUrl();
        when(restTemplate.getForEntity(BASE_URL + "/sitemap_index.xml", String.class))
            .thenReturn(ResponseEntity.ok("sitemap"));
        when(restTemplate.getForObject(BASE_URL + "/robots.txt", String.class)).thenReturn("");
        when(restTemplate.getForObject(BASE_URL, String.class)).thenReturn("<html>ssr rendered</html>");
        Health health = indicator.health();
        assertThat(health.getStatus(), is(Status.DOWN));
        assertThat((String) health.getDetails().get("robots.txt"), containsString("Could not be fetched"));
    }
}
