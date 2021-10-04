/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutMetric2BoxBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.metricsSecurity.BoxMetricsLayoutConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Testing class for the {@link  org.dspace.metricsSecurity.BoxMetricsLayoutConfigurationService} filter
 * @author alba aliu (alb.aliu at atis.al)
 */
public class BoxMetricsLayoutConfigurationIT extends AbstractControllerIntegrationTest {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger
            (BoxMetricsLayoutConfigurationService.class);
    protected BoxMetricsLayoutConfigurationService boxMetricsLayoutConfigurationService;
    @Autowired
    protected CrisLayoutBoxAccessService crisLayoutBoxAccessService;
    @Autowired
    protected CrisLayoutBoxService crisLayoutBoxService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected AuthorizeService authorizeService;
    private Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        boxMetricsLayoutConfigurationService = mock(BoxMetricsLayoutConfigurationService.class,
                withSettings().useConstructor(crisLayoutBoxAccessService,
                        crisLayoutBoxService, itemService, authorizeService));
        when(boxMetricsLayoutConfigurationService.checkPermissionOfMetricByBox(any(),
                any(), any())).thenCallRealMethod();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void hasPermissionAsAdministrator() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem").build();

        try {
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item).build();
            context.setCurrentUser(admin);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlHasPermissionForMetricOnPublicBoxWithAnonymous() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").build();
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder.
                    createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.PUBLIC).build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item)
                    .withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0)
                    .build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.setCurrentUser(null);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlHasPermissionForMetricOnPublicBoxWithLogged() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").build();
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder
                    .createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.OWNER_ONLY).build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item)
                    .withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0)
                    .build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(false, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlHasPermissionForMetricOnPublicBoxWithBoxOwnerUser() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").withCrisOwner(context.getCurrentUser()).build();
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.OWNER_ONLY).build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item).withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0).build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlPermissionsForBoxWithSecurityAdministratorAndUserLoggedIn() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").build();
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.ADMINISTRATOR).build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item).withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0).build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(false, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlPermissionsForBoxWithSecurityOWNER_AND_ADMINISTRATORAndUserAsOwnerAndAdministrator() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").withCrisOwner(admin).build();
            EntityType entityType = EntityTypeBuilder
                    .createEntityTypeBuilder(context, "Publication")
                    .build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder
                    .createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR)
                    .build();
            CrisMetrics metric = CrisMetricsBuilder
                    .createCrisMetrics(context, item)
                    .withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0)
                    .build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.setCurrentUser(admin);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlPermissionsForBoxWithSecurityOWNER_AND_ADMINISTRATORAndUserAsAdministrator() {
        context.turnOffAuthorisationSystem();
        try {
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").build();
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item)
                    .withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0)
                    .build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.setCurrentUser(admin);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void controlPermissionsForBoxWithSecurityCUSTOM_DATA_AND_ADMINISTRATORAndUserAdministrator() {
        context.turnOffAuthorisationSystem();
        try {
            EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
            CrisLayoutBox crisLayoutBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
                    .withType("METRICS").withSecurity(LayoutSecurity.CUSTOM_DATA).build();
            Item item = ItemBuilder.createItem(context, collection).withFullName("TestItem")
                    .withEntityType("Publication").build();
            CrisMetrics metric = CrisMetricsBuilder.createCrisMetrics(context, item).withMetricType("view").build();
            CrisLayoutMetric2Box crisLayoutMetric2Box = CrisLayoutMetric2BoxBuilder
                    .create(context, crisLayoutBox, metric.getMetricType(), 0)
                    .build();
            List<CrisLayoutMetric2Box> crisLayoutMetric2BoxList = new ArrayList<>();
            crisLayoutMetric2BoxList.add(crisLayoutMetric2Box);
            crisLayoutBox.setMetric2box(crisLayoutMetric2BoxList);
            context.setCurrentUser(admin);
            context.restoreAuthSystemState();
            boolean permission = boxMetricsLayoutConfigurationService
                    .checkPermissionOfMetricByBox(context, item, metric);
            assertEquals(true, permission);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage());
            context.restoreAuthSystemState();
        }
    }
}