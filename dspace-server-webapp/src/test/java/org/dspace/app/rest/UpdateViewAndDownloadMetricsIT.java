/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test
 */
public class UpdateViewAndDownloadMetricsIT extends AbstractControllerIntegrationTest {

    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    @Autowired
    private CrisMetricsService crisMetriscService;

    @Autowired
    protected EventService eventService;

    CrisMetrics crisMetrics = null;

    protected static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        configurationService.setProperty("system-event.thread.size", 0);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-statistics.autoCommit", false);
    }


    //test with views and downloads
    @Test
    public void storeCrisMetricsForItemWithViewAndDownloads() throws Exception {
        context.turnOffAuthorisationSystem();
            Community community = CommunityBuilder.createCommunity(context).build();
            parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
            Collection col1 = CollectionBuilder.createCollection(context, community).build();
            Item itemVisited = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A")
                    .inArchive().build();
            Bitstream bitstream_for_item = BitstreamBuilder
                                               .createBitstream(context, itemVisited,
                                                                toInputStream("test", UTF_8))
                                               .withName("bitstream1").build();
            context.restoreAuthSystemState();
            //create view events to store data in statistics
            //visit the publication
            ViewEventRest viewEventRestItem = new ViewEventRest();
            viewEventRestItem.setTargetType("item");
            viewEventRestItem.setTargetId(itemVisited.getID());
            //visit the publication bitstream
            ViewEventRest viewEventRestBitstream = new ViewEventRest();
            viewEventRestBitstream.setTargetType("bitstream");
            viewEventRestBitstream.setTargetId(bitstream_for_item.getID());
            ObjectMapper mapper = new ObjectMapper();
            // add requests for view events
            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestItem))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        //find view and downloads metrics
        CrisMetrics metrics_downloads = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", itemVisited.getID());
        CrisMetrics metrics_views = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", itemVisited.getID());
        assertEquals("view", metrics_views.getMetricType());
        assertEquals("download", metrics_downloads.getMetricType());
        assertEquals(2, metrics_downloads.getMetricCount(), 0);
        assertEquals(1, metrics_views.getMetricCount(), 0);
        assertNull(metrics_downloads.getDeltaPeriod1());
        assertNull(metrics_views.getDeltaPeriod2());
        assertTrue(metrics_views.getLast());
        assertTrue(metrics_downloads.getLast());
    }

    //test only with views
    @Test
    public void storeCrisMetricsForItemWithViews() throws Exception {
        context.turnOffAuthorisationSystem();
            Community community = CommunityBuilder.createCommunity(context).build();
            parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
            Collection col1 = CollectionBuilder.createCollection(context, community).build();
            Item itemVisited = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A")
                    .inArchive().build();
            context.restoreAuthSystemState();
            //create view events to store data in statistics
            //visit the publication
            ViewEventRest viewEventRestItem = new ViewEventRest();
            viewEventRestItem.setTargetType("item");
            viewEventRestItem.setTargetId(itemVisited.getID());
            ObjectMapper mapper = new ObjectMapper();
            // make request for view event
            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestItem))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metrics_views = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", itemVisited.getID());
        // find downloads metric
        CrisMetrics metrics_downloads = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", itemVisited.getID());
        assertEquals("view", metrics_views.getMetricType());
        assertTrue(metrics_views.getLast());
        assertEquals(1, metrics_views.getMetricCount(), 0);
        assertNull(metrics_views.getDeltaPeriod2());
        // must be null because for the item there are not downloads
        assertNull(metrics_downloads);
    }

    //test with previous metrics
    @Test
    public void storeCrisMetricsForItemWithViewAndDownloadsWithExistingValues() throws Exception {
        context.turnOffAuthorisationSystem();
            Community community = CommunityBuilder.createCommunity(context).build();
            parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
            Collection col1 = CollectionBuilder.createCollection(context, community).build();
            Item itemVisited = ItemBuilder.createItem(context, col1)
                    .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                    .withTitle("Title item A")
                    .inArchive().build();
            Bitstream bitstream_for_item = BitstreamBuilder.createBitstream(
                context, itemVisited, toInputStream("test", UTF_8)).withName("bitstream1").build();
            crisMetrics = CrisMetricsBuilder.createCrisMetrics(context, itemVisited)
                    .withMetricType("view")
                    .withMetricCount(1)
                    .isLast(true).build();
            context.restoreAuthSystemState();
            //create view events to store data in statistics
            //visit the publication
            ViewEventRest viewEventRestItem = new ViewEventRest();
            viewEventRestItem.setTargetType("item");
            viewEventRestItem.setTargetId(itemVisited.getID());
            //visit the publication bitstream
            ViewEventRest viewEventRestBitstream = new ViewEventRest();
            viewEventRestBitstream.setTargetType("bitstream");
            viewEventRestBitstream.setTargetId(bitstream_for_item.getID());
            ObjectMapper mapper = new ObjectMapper();
            // make requests for view events
            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestItem))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

            getClient().perform(post("/api/statistics/viewevents")
                    .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                    .contentType(contentType))
                    .andExpect(status().isCreated());

        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metrics_downloads = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", itemVisited.getID());
        CrisMetrics metrics_views = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", itemVisited.getID());
        // find previous metric
        CrisMetrics old_metric = crisMetriscService.find(context, crisMetrics.getID());
        assertEquals("view", metrics_views.getMetricType());
        assertEquals("download", metrics_downloads.getMetricType());
        assertEquals(2, metrics_downloads.getMetricCount(), 0);
        assertEquals(1, metrics_views.getMetricCount(), 0);
        assertNull(metrics_downloads.getDeltaPeriod1());
        assertNull(metrics_views.getDeltaPeriod2());
        assertTrue(metrics_views.getLast());
        assertTrue(metrics_downloads.getLast());
        // previous metric must have last value false
        assertFalse(old_metric.getLast());
    }

    //test with previous week and month views and downloads
    @Test
    public void storeCrisMetricsForItemWithViewAndDownloadsWithPreviousWeekAndMonthValues()
        throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item itemVisited = ItemBuilder.createItem(context, col1)
                                      .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                                      .withTitle("Title item A")
                                      .inArchive().build();
        Bitstream bitstream_for_item = BitstreamBuilder.createBitstream(
            context, itemVisited, toInputStream("test", UTF_8))
                                                       .withName("bitstream1").build();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        // metrics week and a month before for views
        CrisMetrics crisMetrics_previous_month_views = CrisMetricsBuilder.createCrisMetrics(context, itemVisited)
                                                                         .withMetricType("view")
                                                                         .withMetricCount(1)
                                                                         .withAcquisitionDate(cal.getTime())
                                                                         .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views = CrisMetricsBuilder.createCrisMetrics(context, itemVisited)
                                                                        .withMetricType("view")
                                                                        .withMetricCount(1)
                                                                        .withAcquisitionDate(
                                                                            DateUtils.addDays(new Date(), -7))
                                                                        .isLast(true).build();
        // metrics week and a month before for downloads
        CrisMetrics crisMetrics_previous_month_downloads = CrisMetricsBuilder.createCrisMetrics(context, itemVisited)
                                                                             .withMetricType("download")
                                                                             .withMetricCount(2)
                                                                             .withAcquisitionDate(cal.getTime())
                                                                             .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_downloads = CrisMetricsBuilder.createCrisMetrics(context, itemVisited)
                                                                            .withMetricType("download")
                                                                            .withMetricCount(1)
                                                                            .withAcquisitionDate(DateUtils.addDays(
                                                                                new Date(), -7))
                                                                            .isLast(true).build();
        context.restoreAuthSystemState();
        // create view events to store data in statistics
        // visit the publication
        ViewEventRest viewEventRestItem = new ViewEventRest();
        viewEventRestItem.setTargetType("item");
        viewEventRestItem.setTargetId(itemVisited.getID());
        // visit the publication bitstream
        ViewEventRest viewEventRestBitstream = new ViewEventRest();
        viewEventRestBitstream.setTargetType("bitstream");
        viewEventRestBitstream.setTargetId(bitstream_for_item.getID());
        ObjectMapper mapper = new ObjectMapper();
        // add requests for view events
        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRestItem))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                                .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metrics_downloads = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", itemVisited.getID());
        CrisMetrics metrics_views = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", itemVisited.getID());
        // find previous metrics
        CrisMetrics old_metric_views_month =
            crisMetriscService.find(context, crisMetrics_previous_week_views.getID());
        CrisMetrics old_metric_views_week =
            crisMetriscService.find(context, crisMetrics_previous_month_views.getID());
        CrisMetrics old_metric_downloads_month = crisMetriscService.find(context,
                                                                         crisMetrics_previous_month_downloads.getID());
        CrisMetrics old_metric_downloads_week = crisMetriscService.find(context,
                                                                        crisMetrics_previous_week_downloads.getID());
        assertEquals("view", metrics_views.getMetricType());
        assertEquals("download", metrics_downloads.getMetricType());
        assertEquals(2, metrics_downloads.getMetricCount(), 0);
        assertEquals(1, metrics_views.getMetricCount(), 0);
        assertTrue(metrics_views.getLast());
        assertTrue(metrics_downloads.getLast());
        assertTrue(metrics_views.getDeltaPeriod1() == 0);
        assertTrue(metrics_views.getDeltaPeriod2() == 0);
        assertTrue(metrics_downloads.getDeltaPeriod1() == 1);
        assertTrue(metrics_downloads.getDeltaPeriod2() == 0);
        // all last values of previous must be false
        assertFalse(old_metric_views_month.getLast());
        assertFalse(old_metric_views_week.getLast());
        assertFalse(old_metric_downloads_month.getLast());
        assertFalse(old_metric_downloads_week.getLast());
    }


    //test with previous week and month views and downloads for community and items
    @Test
    public void storeCrisMetricsForCommunityAndItemsWithViewWithPreviousWeekAndMonthValues()
            throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                .withTitle("Title item A")
                .inArchive().build();
        Bitstream bitstream_for_item = BitstreamBuilder.createBitstream(
                        context, item, toInputStream("test", UTF_8))
                .withName("bitstream1").build();
        Calendar cal = Calendar.getInstance();
        // metrics week and a month before for views
        cal.add(Calendar.MONTH, -1);
        CrisMetrics crisMetrics_previous_month_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(cal.getTime())
                .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetrics_previous_week_views_item = CrisMetricsBuilder.createCrisMetrics(context, item)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();

        CrisMetrics crisMetrics_previous_week_downloads = CrisMetricsBuilder.createCrisMetrics(context, item)
                .withMetricType("download")
                .withMetricCount(1)
                .withAcquisitionDate(DateUtils.addDays(
                        new Date(), -7))
                .isLast(true).build();
        context.restoreAuthSystemState();
        // create view events to store data in statistics
        // visit the publication
        ViewEventRest viewEventRestComm = new ViewEventRest();
        viewEventRestComm.setTargetType("community");
        viewEventRestComm.setTargetId(community.getID());
        // visit the item
        ViewEventRest viewEventRestItem = new ViewEventRest();
        viewEventRestItem.setTargetType("item");
        viewEventRestItem.setTargetId(item.getID());
        // visit the publication bitstream
        ViewEventRest viewEventRestBitstream = new ViewEventRest();
        viewEventRestBitstream.setTargetType("bitstream");
        viewEventRestBitstream.setTargetId(bitstream_for_item.getID());
        ObjectMapper mapper = new ObjectMapper();
        // view item
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestItem))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        // view comms
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        //download
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                        .contentType(contentType))
                .andExpect(status().isCreated());


        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metric_view_item = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", item.getID());
        CrisMetrics metric_download = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", item.getID());
        CrisMetrics metrics_views_comm = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", community.getID());
        // find previous metrics
        CrisMetrics old_metric_views_month = crisMetriscService.find(context,
                                                                     crisMetrics_previous_month_views_comm.getID());
        CrisMetrics old_metric_views_week = crisMetriscService.find(context,
                                                                    crisMetrics_previous_week_views_comm.getID());
        CrisMetrics old_metric_downloads_week = crisMetriscService.find(context,
                                                                        crisMetrics_previous_week_downloads.getID());


        //control download values
        assertEquals("download", metric_download.getMetricType());
        assertEquals(1, metric_download.getMetricCount(), 0);
        assertTrue(metric_download.getLast());
        assertTrue(metric_download.getDeltaPeriod1() == 0.0);

        assertEquals("view", metrics_views_comm.getMetricType());
        assertTrue(metrics_views_comm.getLast());
        assertEquals(2, metrics_views_comm.getMetricCount(), 0);
        assertTrue(metrics_views_comm.getDeltaPeriod1() == 1.0);
        assertTrue(metrics_views_comm.getDeltaPeriod2() == 1.0);

        assertEquals("view", metric_view_item.getMetricType());
        assertEquals(1, metric_view_item.getMetricCount(), 0);
        assertTrue(metric_view_item.getDeltaPeriod1() == 0.0);
        assertTrue(metric_view_item.getLast());

        // all last values of previous must be false
        assertFalse(old_metric_views_month.getLast());
        assertFalse(old_metric_views_week.getLast());
        assertFalse(old_metric_downloads_week.getLast());

    }

    //test with previous week and month views and downloads for community collection and items together
    @Test
    public void storeCrisMetricsForCommunityItemsAndCollectionWithViewWithPreviousWeekAndMonthValues()
            throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                .withTitle("Title item A")
                .inArchive().build();
        Bitstream bitstream_for_item = BitstreamBuilder.createBitstream(
                        context, item, toInputStream("test", UTF_8))
                .withName("bitstream1").build();
        Calendar cal = Calendar.getInstance();
        // metrics week and a month before for views
        cal.add(Calendar.MONTH, -1);
        CrisMetrics crisMetrics_previous_month_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(cal.getTime())
                .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // collections
        CrisMetrics crisMetrics_previous_month_views_col = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(3)
                .withAcquisitionDate(cal.getTime())
                .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views_col = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // items
        CrisMetrics crisMetrics_previous_week_views_item = CrisMetricsBuilder.createCrisMetrics(context, item)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();

        CrisMetrics crisMetrics_previous_week_downloads = CrisMetricsBuilder.createCrisMetrics(context, item)
                .withMetricType("download")
                .withMetricCount(1)
                .withAcquisitionDate(DateUtils.addDays(
                        new Date(), -7))
                .isLast(true).build();
        context.restoreAuthSystemState();
        // create view events to store data in statistics
        // visit the publication
        ViewEventRest viewEventRestComm = new ViewEventRest();
        viewEventRestComm.setTargetType("community");
        viewEventRestComm.setTargetId(community.getID());
        // visit the collection
        ViewEventRest viewEventRestColl = new ViewEventRest();
        viewEventRestColl.setTargetType("collection");
        viewEventRestColl.setTargetId(col1.getID());
        // visit the item
        ViewEventRest viewEventRestItem = new ViewEventRest();
        viewEventRestItem.setTargetType("item");
        viewEventRestItem.setTargetId(item.getID());
        // visit the publication bitstream
        ViewEventRest viewEventRestBitstream = new ViewEventRest();
        viewEventRestBitstream.setTargetType("bitstream");
        viewEventRestBitstream.setTargetId(bitstream_for_item.getID());
        ObjectMapper mapper = new ObjectMapper();
        // view item
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestItem))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        // view comms
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());

        // view cols
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestColl))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestColl))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        //download
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestBitstream))
                        .contentType(contentType))
                .andExpect(status().isCreated());

        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metric_view_item = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", item.getID());
        CrisMetrics metric_download = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "download", item.getID());
        CrisMetrics metrics_views_comm = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", community.getID());
        CrisMetrics metrics_views_cols = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", col1.getID());
        // find previous metrics
        CrisMetrics old_metric_views_month = crisMetriscService.find(context,
                                                                     crisMetrics_previous_month_views_comm.getID());
        CrisMetrics old_metric_views_week = crisMetriscService.find(context,
                                                                    crisMetrics_previous_week_views_comm.getID());
        CrisMetrics old_metric_downloads_week = crisMetriscService.find(context,
                                                                        crisMetrics_previous_week_downloads.getID());
        CrisMetrics old_metric_view_week_items = crisMetriscService.find(context,
                                                                         crisMetrics_previous_week_views_item.getID());
        CrisMetrics old_metric_view_week_col = crisMetriscService.find(context,
                                                                       crisMetrics_previous_week_views_col.getID());
        CrisMetrics old_metric_view_month_col = crisMetriscService.find(context,
                                                                        crisMetrics_previous_month_views_col.getID());

        //control download values
        assertEquals("download", metric_download.getMetricType());
        assertEquals(1, metric_download.getMetricCount(), 0);
        assertTrue(metric_download.getLast());
        assertTrue(metric_download.getDeltaPeriod1() == 0.0);

        assertEquals("view", metrics_views_comm.getMetricType());
        assertTrue(metrics_views_comm.getLast());
        assertEquals(2, metrics_views_comm.getMetricCount(), 0);
        assertTrue(metrics_views_comm.getDeltaPeriod1() == 1.0);
        assertTrue(metrics_views_comm.getDeltaPeriod2() == 1.0);

        assertEquals("view", metric_view_item.getMetricType());
        assertEquals(1, metric_view_item.getMetricCount(), 0);
        assertTrue(metric_view_item.getDeltaPeriod1() == 0.0);
        assertTrue(metric_view_item.getLast());


        assertEquals("view", metrics_views_cols.getMetricType());
        assertEquals(2, metrics_views_cols.getMetricCount(), 0);
        assertTrue(metrics_views_cols.getDeltaPeriod1() == 1.0);
        assertTrue(metrics_views_cols.getDeltaPeriod2() == -1.0);
        assertTrue(metrics_views_cols.getLast());


        // all last values of previous must be false
        assertFalse(old_metric_views_month.getLast());
        assertFalse(old_metric_views_week.getLast());
        assertFalse(old_metric_downloads_week.getLast());
        assertFalse(old_metric_view_week_items.getLast());
        assertFalse(old_metric_view_week_col.getLast());
        assertFalse(old_metric_view_month_col.getLast());
    }

    //test with previous week and month views and downloads for community and collection
    @Test
    public void storeCrisMetricsForCommunityAndCollectionWithViewWithPreviousWeekAndMonthValues()
            throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, col1)
                .withDoiIdentifier("10.1016/j.gene.2009.04.019")
                .withTitle("Title item A")
                .inArchive().build();
        Calendar cal = Calendar.getInstance();
        // metrics week and a month before for views
        cal.add(Calendar.MONTH, -1);
        CrisMetrics crisMetrics_previous_month_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(cal.getTime())
                .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views_comm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(1)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // collections
        CrisMetrics crisMetrics_previous_month_views_col = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(3)
                .withAcquisitionDate(cal.getTime())
                .isLast(false).build();
        CrisMetrics crisMetrics_previous_week_views_col = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();

        context.restoreAuthSystemState();
        // create view events to store data in statistics
        // visit the publication
        ViewEventRest viewEventRestComm = new ViewEventRest();
        viewEventRestComm.setTargetType("community");
        viewEventRestComm.setTargetId(community.getID());
        // visit the collection
        ViewEventRest viewEventRestColl = new ViewEventRest();
        viewEventRestColl.setTargetType("collection");
        viewEventRestColl.setTargetId(col1.getID());

        ObjectMapper mapper = new ObjectMapper();
        // view comms
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestComm))
                        .contentType(contentType))
                .andExpect(status().isCreated());

        // view cols
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestColl))
                        .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                        .content(mapper.writeValueAsBytes(viewEventRestColl))
                        .contentType(contentType))
                .andExpect(status().isCreated());


        String[] args = new String[] {"store-metrics"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        int status = handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertEquals(0, status);
        CrisMetrics metrics_views_comm = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", community.getID());
        CrisMetrics metrics_views_cols = crisMetriscService.findLastMetricByResourceIdAndMetricsTypes(
            context, "view", col1.getID());
        // find previous metrics
        CrisMetrics old_metric_views_month = crisMetriscService.find(context,
                                                                     crisMetrics_previous_month_views_comm.getID());
        CrisMetrics old_metric_views_week = crisMetriscService.find(context,
                                                                    crisMetrics_previous_week_views_comm.getID());
        CrisMetrics old_metric_view_week_col = crisMetriscService.find(context,
                                                                       crisMetrics_previous_week_views_col.getID());
        CrisMetrics old_metric_view_month_col = crisMetriscService.find(context,
                                                                        crisMetrics_previous_month_views_col.getID());


        assertEquals("view", metrics_views_comm.getMetricType());
        assertTrue(metrics_views_comm.getLast());
        assertEquals(2, metrics_views_comm.getMetricCount(), 0);
        assertTrue(metrics_views_comm.getDeltaPeriod1() == 1.0);
        assertTrue(metrics_views_comm.getDeltaPeriod2() == 0);

        assertEquals("view", metrics_views_cols.getMetricType());
        assertEquals(2, metrics_views_cols.getMetricCount(), 0);
        assertTrue(metrics_views_cols.getDeltaPeriod1() == 0);
        assertTrue(metrics_views_cols.getDeltaPeriod2() == -1.0);
        assertTrue(metrics_views_cols.getLast());

        // all last values of previous must be false
        assertFalse(old_metric_views_month.getLast());
        assertFalse(old_metric_views_week.getLast());
        assertFalse(old_metric_view_week_col.getLast());
        assertFalse(old_metric_view_month_col.getLast());
    }
}
