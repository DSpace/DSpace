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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
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

import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.dspace.metrics.wos.UpdateWOSMetrics;
import org.dspace.metrics.wos.UpdateWOSPersonMetrics;
import org.dspace.metrics.wos.WOSPersonRestConnector;
import org.dspace.metrics.wos.WOSRestConnector;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class UpdateWOSMetricsIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CrisMetricsService crisMetriscService;

    @Autowired
    private WOSRestConnector wosRestConnector;

    @Autowired
    private WOSPersonRestConnector wosPersonRestConnector;

    private CrisMetrics crisMetrics = null;

    @Test
    public void updateCrisMetricsFromWosMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosResponceJSON").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Publication")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withDoiIdentifier("10.1016/j.jinf.2020.06.024").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                                    .withMetricCount(23)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics metric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                     UpdateWOSMetrics.WOS_METRIC_TYPE, itemA.getID());

            assertNotEquals(metric1.getID(), metric.getID());
            assertFalse(metric1.getLast());
            assertTrue(metric.getLast());
            assertEquals(87, metric.getMetricCount(), 0);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void wosResponceMetricCountAbsentMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get(
                        "test.wosResponceMetricCountAbsent").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Publication")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withDoiIdentifier("10.1016/j.ji").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                                    .withMetricCount(2312)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics metric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                     UpdateWOSMetrics.WOS_METRIC_TYPE, itemA.getID());

            assertEquals(metric1.getID(), metric.getID());
            String restId = CrisMetricsBuilder.getRestStoredMetricId(metric.getID());
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(get("/api/cris/metrics/" + restId))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$", is(CrisMetricsMatcher.matchCrisMetrics(metric))))
                                 .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                                                     "/api/cris/metrics/" + restId)));
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void badRequestMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosBadRequest").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Publication")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withDoiIdentifier("10.1016/j.ji").build();

            CrisMetrics metric1 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                                    .withMetricCount(2312)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics metric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                     UpdateWOSMetrics.WOS_METRIC_TYPE, itemA.getID());

            assertEquals(metric1.getID(), metric.getID());

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsWithPersonEntityTypeMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosPersonRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosResponceMultiRecords").toString())) {

            String wosMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosPersonRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(wosMetricsExample, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withOrcidIdentifier("0000-0001-8190-0000").build();

            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE)
                                                    .withMetricCount(22)
                                                    .isLast(true).build();

            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime( new Date());
            calendar2.add(Calendar.DATE, -7);
            calendar2.set(Calendar.HOUR_OF_DAY, 10);

            Date oneWeekAgo = calendar2.getTime();

            CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneWeekAgo)
                                                     .withMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE)
                                                     .withMetricCount(17)
                                                     .isLast(false).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime( new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneMonthAgo)
                                                     .withMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE)
                                                     .withMetricCount(10)
                                                     .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos-person" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics wosMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                                       UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE, itemA.getID());

            assertNotEquals(metric.getId(), wosMetric.getId());
            assertEquals(wosMetric.getMetricCount(), 280, 0);
            assertEquals(wosMetric.getMetricType(), UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE);
            assertEquals(wosMetric.getMetricCount() - metrics2.getMetricCount(), wosMetric.getDeltaPeriod1(), 0);
            assertEquals(wosMetric.getMetricCount() - metrics3.getMetricCount(), wosMetric.getDeltaPeriod2(), 0);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosPersonRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsWithPersonEntityTypeBadRequestMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosPersonRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosBadRequest").toString())) {

            String wosMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosPersonRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(wosMetricsExample, 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withOrcidIdentifier("0000").build();

            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE)
                                                    .withMetricCount(22)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos-person" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics wosMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE, itemA.getID());

            assertEquals(metric.getId(), wosMetric.getId());
            assertEquals(wosMetric.getMetricCount(), 22, 0);
            assertEquals(wosMetric.getMetricType(), UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosPersonRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsWithPersonEntityTypeUnauthorizedMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosPersonRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosUnauthorized").toString())) {

            String wosMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosPersonRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(wosMetricsExample, 401, "Unauthorized");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Person")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withOrcidIdentifier("0000-0001-8190-0000").build();

            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                    .withMetricType(UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE)
                                                    .withMetricCount(22)
                                                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos-person" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics wosMetric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                    UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE, itemA.getID());

            assertEquals(metric.getId(), wosMetric.getId());
            assertEquals(wosMetric.getMetricCount(), 22, 0);
            assertEquals(wosMetric.getMetricType(), UpdateWOSPersonMetrics.WOS_PERSON_METRIC_TYPE);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosPersonRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsFromWosWithDeltaPeriodValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = wosRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.wosResponceJSON").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            wosRestConnector.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                               .withEntityType("Publication")
                                               .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                               .withTitle("Title item A")
                               .withDoiIdentifier("10.1016/j.jinf.2020.06.024").build();

            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.YEAR, 2021);
            calendar.set(Calendar.MONTH, 2);
            calendar.set(Calendar.DATE, 1);

            Date date = calendar.getTime();

            crisMetrics = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                            .withAcquisitionDate(date)
                                            .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                            .withMetricCount(23)
                                            .isLast(true).build();

            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime( new Date());
            calendar2.add(Calendar.DATE, -7);
            calendar2.set(Calendar.HOUR_OF_DAY, 10);

            Date oneWeekAgo = calendar2.getTime();

            CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneWeekAgo)
                                                     .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                                     .withMetricCount(17)
                                                     .isLast(false).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime( new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                                                     .withAcquisitionDate(oneMonthAgo)
                                                     .withMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE)
                                                     .withMetricCount(4)
                                                     .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[] { "update-metrics", "-s", "wos" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            assertEquals(0, handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin));

            CrisMetrics metric = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(context,
                                     UpdateWOSMetrics.WOS_METRIC_TYPE, itemA.getID());

            assertNotEquals(crisMetrics.getID(), metric.getID());
            assertFalse(crisMetrics.getLast());
            assertTrue(metric.getLast());
            assertEquals(87, metric.getMetricCount(), 0);
            assertEquals(metric.getMetricCount() - metrics2.getMetricCount(), metric.getDeltaPeriod1(), 0);
            assertEquals(metric.getMetricCount() - metrics3.getMetricCount(), metric.getDeltaPeriod2(), 0);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            wosRestConnector.setHttpClient(originalHttpClient);
        }
    }

    private CloseableHttpResponse mockResponse(String xmlExample, int statusCode, String reason)
        throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(xmlExample));

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