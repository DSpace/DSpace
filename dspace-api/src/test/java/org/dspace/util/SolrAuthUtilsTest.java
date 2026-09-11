/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SolrAuthUtils}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrAuthUtilsTest {

    @Mock
    private ConfigurationService configurationService;

    @Test
    public void testGetAuthorizationHeaderValueWhenTypeNotSet() {
        assertNull(SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueWhenTypeBlank() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("");
        assertNull(SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueWhenTypeIsNone() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("none");
        assertNull(SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueWhenTypeUnrecognized() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("digest");
        assertNull(SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueWhenBasicButUserBlank() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("solr.authentication.user")).thenReturn(null);
        assertNull(SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueWhenBasicConfigured() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("solr.authentication.user")).thenReturn("solr");
        when(configurationService.getProperty("solr.authentication.password")).thenReturn("SolrRocks");

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("solr:SolrRocks".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueTreatsNullPasswordAsEmpty() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("solr.authentication.user")).thenReturn("solr");
        when(configurationService.getProperty("solr.authentication.password")).thenReturn(null);

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("solr:".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, SolrAuthUtils.getAuthorizationHeaderValue("solr", null, configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueUsesFallbackWhenPrimaryNotSet() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("solr.authentication.user")).thenReturn("solr");
        when(configurationService.getProperty("solr.authentication.password")).thenReturn("SolrRocks");

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("solr:SolrRocks".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected,
                SolrAuthUtils.getAuthorizationHeaderValue("iiif.search", "solr", configurationService));
    }

    @Test
    public void testGetAuthorizationHeaderValueIgnoresFallbackWhenPrimarySet() {
        when(configurationService.getProperty("iiif.search.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("iiif.search.authentication.user")).thenReturn("iiif-solr");
        when(configurationService.getProperty("iiif.search.authentication.password")).thenReturn("iiif-pass");

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("iiif-solr:iiif-pass".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected,
                SolrAuthUtils.getAuthorizationHeaderValue("iiif.search", "solr", configurationService));
        // The fallback prefix must never be consulted once the primary prefix is fully configured.
        verify(configurationService, never()).getProperty("solr.authentication.type");
    }

    @Test
    public void testAddAuthenticationIfConfiguredAddsInterceptorWhenConfigured() {
        when(configurationService.getProperty("solr.authentication.type")).thenReturn("basic");
        when(configurationService.getProperty("solr.authentication.user")).thenReturn("solr");
        when(configurationService.getProperty("solr.authentication.password")).thenReturn("SolrRocks");

        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        SolrAuthUtils.addAuthenticationIfConfigured(builder, "solr", configurationService);

        verify(builder).addInterceptorFirst(any(HttpRequestInterceptor.class));
    }

    @Test
    public void testAddAuthenticationIfConfiguredNoOpWhenNotConfigured() {
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        SolrAuthUtils.addAuthenticationIfConfigured(builder, "solr", configurationService);

        verifyNoInteractions(builder);
    }
}