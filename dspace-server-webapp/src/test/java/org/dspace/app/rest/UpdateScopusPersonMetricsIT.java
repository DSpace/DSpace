/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.matcher.CrisMetricsMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.metrics.scopus.ScopusPersonProvider.ScopusPersonMetric;
import org.dspace.metrics.scopus.ScopusPersonRestConnector;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.utils.StringInputStream;

/**
* @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
* @author Corrado Lombardi (corrado.lombardi at 4science.it)
*/
public class UpdateScopusPersonMetricsIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CrisMetricsService crisMetriscService;

    @Autowired
    private ScopusPersonRestConnector scopusPersonRestConnector;

    @Test
    public void updateHindexCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("ENHANCED");

            CloseableHttpResponse response = mockResponse(jsonExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12)
                                                    .isLast(true).build();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime( new Date());
            calendar.add(Calendar.DATE, -7);
            calendar.set(Calendar.HOUR_OF_DAY, 10);

            Date oneWeekAgo = calendar.getTime();

            CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneWeekAgo)
                                                     .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                     .withMetricCount(10)
                                                     .isLast(false).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime( new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneMonthAgo)
                                                     .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                     .withMetricCount(7)
                                                     .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newHIndexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertNotEquals(newHIndexMetric.getID(), metric.getID());
            assertEquals(newHIndexMetric.getMetricCount() - metrics2.getMetricCount(),
                         newHIndexMetric.getDeltaPeriod1(), 0);
            assertEquals(newHIndexMetric.getMetricCount() - metrics3.getMetricCount(),
                         newHIndexMetric.getDeltaPeriod2(), 0);

            String restId = CrisMetricsBuilder.getRestStoredMetricId(newHIndexMetric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newHIndexMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateCitedCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("ENHANCED");

            CloseableHttpResponse response = mockResponse(jsonExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.CITED.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime( new Date());
            calendar.add(Calendar.DATE, -7);
            calendar.set(Calendar.HOUR_OF_DAY, 10);

            Date oneWeekAgo = calendar.getTime();

            CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneWeekAgo)
                                                     .withMetricType(ScopusPersonMetric.CITED.metricType())
                                                     .withMetricCount(8000)
                                                     .isLast(false).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime( new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneMonthAgo)
                                                     .withMetricType(ScopusPersonMetric.CITED.metricType())
                                                     .withMetricCount(2000)
                                                     .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCitedMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.CITED.metricType(), itemA.getID());

            assertNotEquals(newCitedMetric.getID(), metric1.getID());
            assertEquals(newCitedMetric.getMetricCount() - metrics2.getMetricCount(),
                         newCitedMetric.getDeltaPeriod1(), 0);
            assertEquals(newCitedMetric.getMetricCount() - metrics3.getMetricCount(),
                         newCitedMetric.getDeltaPeriod2(), 0);

            String restId = CrisMetricsBuilder.getRestStoredMetricId(newCitedMetric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCitedMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateDocumentCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("ENHANCED");


            CloseableHttpResponse response = mockResponse(jsonExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.DOCUMENT.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newDocumentMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.DOCUMENT.metricType(), itemA.getID());

            assertNotEquals(newDocumentMetric.getID(), metric1.getID());
            String restId = CrisMetricsBuilder.getRestStoredMetricId(newDocumentMetric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newDocumentMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateCitationCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("ENHANCED");

            CloseableHttpResponse response = mockResponse(jsonExample, 200, "OK");

            when(httpClient.execute(matchUri("www.testurl.org/7406754790?view=ENHANCED"))).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.CITATION.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCitationMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.CITATION.metricType(), itemA.getID());

            assertNotEquals(newCitationMetric.getID(), metric1.getID());
            String restId = CrisMetricsBuilder.getRestStoredMetricId(newCitationMetric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCitationMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateCoauthorCrisMetricMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexMetrics").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("");

            CloseableHttpResponse response = mockResponse(jsonExample, 200, "OK");

            when(httpClient.execute(matchUri("www.testurl.org/7406754790"))).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.COAUTHOR.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics newCoauthorMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.COAUTHOR.metricType(), itemA.getID());

            assertNotEquals(newCoauthorMetric.getID(), metric1.getID());
            String restId = CrisMetricsBuilder.getRestStoredMetricId(newCoauthorMetric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(newCoauthorMetric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateHIndexCrisMetricNotFoundResourceMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexResouceNotFound").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode(null);

            CloseableHttpResponse response = mockResponse(jsonExample, 404, "Not Found");
            when(httpClient.execute(matchUri("www.testurl.org/7406754790"))).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics hindexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertEquals(hindexMetric.getID(), metric1.getID());
            assertEquals(hindexMetric.getMetricCount(), metric1.getMetricCount(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    @Test
    public void updateHIndexCrisMetricApiKeyInvalidMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusPersonRestConnector.getHttpClient();
        String originalViewMode = scopusPersonRestConnector.getViewMode();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.hindexApiKeyInvalid").toString())) {

            String jsonExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusPersonRestConnector.setHttpClient(httpClient);
            scopusPersonRestConnector.setUrl("www.testurl.org/");
            scopusPersonRestConnector.setViewMode("TEST");

            CloseableHttpResponse response = mockResponse(jsonExample, 401, "Unauthorized");

            when(httpClient.execute(matchUri("www.testurl.org/7406754790?view=TEST"))).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withScopusAuthorIdentifier("7406754790").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(ScopusPersonMetric.H_INDEX.metricType())
                                                    .withMetricCount(12000)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "scopus-person"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics hindexMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    ScopusPersonMetric.H_INDEX.metricType(), itemA.getID());

            assertEquals(hindexMetric.getID(), metric1.getID());
            assertEquals(hindexMetric.getMetricCount(), metric1.getMetricCount(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusPersonRestConnector.setHttpClient(originalHttpClient);
            scopusPersonRestConnector.setViewMode(originalViewMode);
            scopusPersonRestConnector.setUrl("");
        }
    }

    private HttpUriRequest matchUri(String uri) {
        return ArgumentMatchers.argThat(uriRequest -> uriRequest.getURI().toString().equals(uri));
    }

    private CloseableHttpResponse mockResponse(String jsonExample, int statusCode, String reason)
        throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(jsonExample));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine())
            .thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    private StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}