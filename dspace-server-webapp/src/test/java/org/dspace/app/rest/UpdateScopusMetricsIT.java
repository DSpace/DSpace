/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
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
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.metrics.scopus.ScopusRestConnector;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk - 4Science
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class UpdateScopusMetricsIT extends AbstractControllerIntegrationTest {
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    @Autowired
    private CrisMetricsService crisMetriscService;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    private ScopusRestConnector scopusRestConnector;
    CrisMetrics crisMetrics = null;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-statistics.autoCommit", false);
    }

    @Test
    public void updateCrisMetricsMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.scopusMetricsXML").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusRestConnector.setHttpClient(httpClient);

            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(new StringInputStream(xmlMetricsExample));

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine())
                    .thenReturn(statusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
            when(response.getEntity()).thenReturn(basicHttpEntity);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Publication")
                    .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A").build();

            crisMetrics = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(4)
                    .isLast(true).build();

            context.restoreAuthSystemState();

            String[] args = new String[]{"update-metrics", "-s", "scopus"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

            assertEquals(0, status);

            CrisMetrics metrics = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
                    context, UpdateScopusMetrics.SCOPUS_CITATION, itemA.getID());

            assertEquals(UpdateScopusMetrics.SCOPUS_CITATION, metrics.getMetricType());
            assertEquals(44, metrics.getMetricCount(), 0);

            String remark = "{\"identifier\":\"2-s2.0-67349162500\",\"detailUrl\":"
                    + "\"https://www.scopus.com/inward/citedby.uri?"
                    + "partnerID\\u003dHzOxMe3b\\u0026scp\\u003d67349162500\\u0026origin\\u003dinward\""
                    + ",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";
            assertEquals(remark, metrics.getRemark());
            assertNull(metrics.getDeltaPeriod1());
            assertNull(metrics.getDeltaPeriod2());
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsIdNotExistMockitoTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.scopusMetricsNotExistXML").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusRestConnector.setHttpClient(httpClient);

            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(new StringInputStream(xmlMetricsExample));

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine())
                    .thenReturn(statusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Publication")
                    .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/33")
                    .withTitle("Title item A").build();

            context.restoreAuthSystemState();

            String[] args = new String[]{"update-metrics", "-s", "scopus"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

            assertEquals(0, status);

            CrisMetrics metrics = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
                    context, UpdateScopusMetrics.SCOPUS_CITATION, itemA.getID());
            assertNull(metrics);
        } finally {
            scopusRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsWithDeltaPeriodValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.scopusMetricsXML").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusRestConnector.setHttpClient(httpClient);

            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(new StringInputStream(xmlMetricsExample));

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine())
                    .thenReturn(statusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
            when(response.getEntity()).thenReturn(basicHttpEntity);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Publication")
                    .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A").build();

            Calendar calendar2 = Calendar.getInstance();

            calendar2.set(Calendar.YEAR, 2021);
            calendar2.set(Calendar.MONTH, 2);
            calendar2.set(Calendar.DATE, 1);

            Date date = calendar2.getTime();

            crisMetrics = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withAcquisitionDate(date)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(20)
                    .isLast(true).build();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, -7);
            calendar.set(Calendar.HOUR_OF_DAY, 10);

            Date oneWeekAgo = calendar.getTime();

            CrisMetrics metrics2 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withAcquisitionDate(oneWeekAgo)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(17)
                    .isLast(false).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime(new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withAcquisitionDate(oneMonthAgo)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(4)
                    .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[]{"update-metrics", "-s", "scopus"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

            assertEquals(0, status);

            CrisMetrics metrics = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
                    context, UpdateScopusMetrics.SCOPUS_CITATION, itemA.getID());

            assertEquals(UpdateScopusMetrics.SCOPUS_CITATION, metrics.getMetricType());
            assertEquals(44, metrics.getMetricCount(), 0);

            String remark = "{\"identifier\":\"2-s2.0-67349162500\",\"detailUrl\":"
                    + "\"https://www.scopus.com/inward/citedby.uri?"
                    + "partnerID\\u003dHzOxMe3b\\u0026scp\\u003d67349162500\\u0026origin\\u003dinward\""
                    + ",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";
            assertEquals(remark, metrics.getRemark());
            assertEquals(metrics.getMetricCount() - metrics2.getMetricCount(), metrics.getDeltaPeriod1(), 0);
            assertEquals(metrics.getMetricCount() - metrics3.getMetricCount(), metrics.getDeltaPeriod2(), 0);
        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusRestConnector.setHttpClient(originalHttpClient);
        }
    }

    @Test
    public void updateCrisMetricsWithoutDeltaPerio1Test() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = scopusRestConnector.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        Item itemA = null;
        try (FileInputStream file = new FileInputStream(testProps.get("test.scopusMetricsXML").toString())) {

            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            scopusRestConnector.setHttpClient(httpClient);

            BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
            basicHttpEntity.setChunked(true);
            basicHttpEntity.setContent(new StringInputStream(xmlMetricsExample));

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getStatusLine())
                    .thenReturn(statusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
            when(response.getEntity()).thenReturn(basicHttpEntity);

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .withName("Parent Community").build();

            Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Publication")
                    .withName("Collection 1").build();

            itemA = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A").build();

            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.YEAR, 2021);
            calendar.set(Calendar.MONTH, 2);
            calendar.set(Calendar.DATE, 1);

            Date date = calendar.getTime();

            crisMetrics = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withAcquisitionDate(date)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(20)
                    .isLast(true).build();

            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime(new Date());
            calendar3.add(Calendar.MONTH, -1);
            calendar3.set(Calendar.HOUR_OF_DAY, 21);

            Date oneMonthAgo = calendar3.getTime();

            CrisMetrics metrics3 = CrisMetricsBuilder.createCrisMetrics(context, itemA)
                    .withAcquisitionDate(oneMonthAgo)
                    .withMetricType(UpdateScopusMetrics.SCOPUS_CITATION)
                    .withMetricCount(4)
                    .isLast(false).build();

            context.restoreAuthSystemState();

            String[] args = new String[]{"update-metrics", "-s", "scopus"};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);

            assertEquals(0, status);

            CrisMetrics metrics = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
                    context, UpdateScopusMetrics.SCOPUS_CITATION, itemA.getID());

            assertEquals(UpdateScopusMetrics.SCOPUS_CITATION, metrics.getMetricType());
            assertEquals(44, metrics.getMetricCount(), 0);

            String remark = "{\"identifier\":\"2-s2.0-67349162500\",\"detailUrl\":"
                    + "\"https://www.scopus.com/inward/citedby.uri?"
                    + "partnerID\\u003dHzOxMe3b\\u0026scp\\u003d67349162500\\u0026origin\\u003dinward\""
                    + ",\"pmid\":\"19406218\",\"doi\":\"10.1016/j.gene.2009.04.019\"}";

            assertEquals(remark, metrics.getRemark());
            assertNull(metrics.getDeltaPeriod1());
            assertEquals(metrics.getMetricCount() - metrics3.getMetricCount(), metrics.getDeltaPeriod2(), 0);

        } finally {
            CrisMetricsBuilder.deleteCrisMetrics(itemA);
            scopusRestConnector.setHttpClient(originalHttpClient);
        }
    }

    private StatusLine statusLine(final ProtocolVersion protocolVersion, int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return protocolVersion;
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
