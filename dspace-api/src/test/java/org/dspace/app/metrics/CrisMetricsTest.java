/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metrics;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.metrics.service.CrisMetricsServiceImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CrisMetricsTest extends AbstractUnitTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CrisMetricsTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected CrisMetricsService crisMetricsService = new DSpace().getServiceManager().getServiceByName(
                                 CrisMetricsServiceImpl.class.getName(), CrisMetricsServiceImpl.class);

    private CrisMetrics metrics1;
    private CrisMetrics metrics2;
    private CrisMetrics metrics3;
    private Community community = null;
    private Collection collection = null;
    private Item item = null;
    protected WorkspaceItem wsi = null;
    private Item item2 = null;
    protected WorkspaceItem wsi2 = null;

    @Before
    @Override
    public void init() {
        super.init();
        context.turnOffAuthorisationSystem();
        try {
            this.community = communityService.create(null, context);
            this.collection = collectionService.create(context, this.community);

            wsi = workspaceItemService.create(context, collection, false);
            item = wsi.getItem();
            metrics1 = crisMetricsService.create(context, item);
            metrics1.setMetricType("Citation");
            metrics1.setMetricCount(Double.valueOf(5));
            metrics1.setLast(true);

            wsi2 = workspaceItemService.create(context, collection, false);
            item2 = wsi2.getItem();
            metrics2 = crisMetricsService.create(context, item2);
            metrics2.setMetricType("SomeMeticsType");
            metrics2.setMetricCount(Double.valueOf(2));
            metrics2.setLast(false);

            metrics3 = crisMetricsService.create(context, item2);
            metrics3.setMetricType("OtherMeticsType");
            metrics3.setLast(true);

            context.commit();
        } catch (SQLException | AuthorizeException ex) {
            log.error("Error in init", ex);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @After
    @Override
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            if (metrics1 != null) {
                metrics1 = context.reloadEntity(metrics1);
                crisMetricsService.delete(context, metrics1);
                metrics1 = null;
            }
            if (metrics2 != null) {
                metrics2 = context.reloadEntity(metrics2);
                crisMetricsService.delete(context, metrics2);
                metrics2 = null;
            }
            if (metrics3 != null) {
                metrics3 = context.reloadEntity(metrics3);
                crisMetricsService.delete(context, metrics3);
                metrics3 = null;
            }
            context.commit();
            context.restoreAuthSystemState();
            super.destroy();
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
    }

    @Test
    public void findAllTest() throws SQLException {
        List<CrisMetrics> metrics = crisMetricsService.findAll(context);
        assertEquals("findAll", 3, metrics.size());
    }

    @Test
    public void countAllTest() throws SQLException {
        int metrics = crisMetricsService.count(context);
        assertEquals(3, metrics);
    }

    @Test
    public void findLastMetricByResourceIdAndMetricsTypesTest() throws SQLException {
        CrisMetrics crisMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(
                                  context, "Citation", item.getID());
        assertNotNull(crisMetrics);
        assertTrue(crisMetrics.getLast());
        assertEquals(metrics1.getMetricType(), crisMetrics.getMetricType());
        assertEquals(metrics1.getMetricCount(), crisMetrics.getMetricCount(), 0);
        assertEquals(metrics1.getLast(), crisMetrics.getLast());

        CrisMetrics crisMetrics2 = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(
                                   context, "OtherMeticsType", item2.getID());
        assertNotNull(crisMetrics2);
        assertTrue(crisMetrics2.getLast());
        assertEquals(metrics3.getMetricType(), crisMetrics2.getMetricType());
        assertNull(crisMetrics2.getMetricCount());
        assertEquals(metrics3.getLast(), crisMetrics2.getLast());
    }

    @Test
    public void findLastMetricByResourceIdAndMetricsTypesIsNotLastTest() throws SQLException {
        CrisMetrics crisMetrics = crisMetricsService
                                 .findLastMetricByResourceIdAndMetricsTypes(context, "SomeMeticsType", item2.getID());
        assertNull("There is no metric with these characteristics", crisMetrics);
    }
}