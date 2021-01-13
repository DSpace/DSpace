/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.metrics.CrisItemMetricsService;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CrisLayoutBoxServiceImpl, so far only findByItem method is tested.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

@RunWith(MockitoJUnitRunner.class)
public class CrisLayoutBoxServiceImplTest {

    private CrisLayoutBoxServiceImpl crisLayoutBoxService;

    @Mock
    private Context context;
    @Mock
    private CrisLayoutBoxDAO dao;
    @Mock
    private ItemService itemService;
    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private EntityTypeService entityTypeService;
    @Mock
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;
    @Mock
    private CrisItemMetricsService crisItemMetricsService;

    @Before
    public void setUp() throws Exception {
        crisLayoutBoxService = new CrisLayoutBoxServiceImpl(dao, itemService, authorizeService, entityTypeService,
                                                            crisLayoutBoxAccessService, crisItemMetricsService);
    }

    @Test(expected = NullPointerException.class)
    public void nullItemThrowsException() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(null);

        crisLayoutBoxService.findByItem(context, itemUuid, tabId);
    }

    @Test
    public void nullBoxesReturnsEmptyList() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        Item item = Mockito.mock(Item.class);

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(null);

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        assertThat(boxes, is(emptyList()));
    }

    @Test
    public void emptyBoxesReturnsEmptyList() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        Item item = Mockito.mock(Item.class);

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(emptyList());

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        assertThat(boxes, is(emptyList()));
    }

    @Test
    public void nullItemMetadataReturnsEmptyList() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        Item item = Mockito.mock(Item.class);

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(singletonList(new CrisLayoutBox()));

        when(item.getMetadata()).thenReturn(null);

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        assertThat(boxes, is(emptyList()));
    }

    @Test
    public void emptyItemMetadataReturnsEmptyList() throws Exception {
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        Item item = Mockito.mock(Item.class);

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(singletonList(new CrisLayoutBox()));

        when(item.getMetadata()).thenReturn(emptyList());

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        assertThat(boxes, is(emptyList()));
    }

    @Test
    public void onlyMatchingMetadataAreReturned() throws Exception {
        Item item = Mockito.mock(Item.class);
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        MetadataField fooMetadata = mock(MetadataField.class);
        MetadataField barMetadata = mock(MetadataField.class);
        MetadataField bazMetadata = mock(MetadataField.class);
        MetadataField notDisplayedMetadata = mock(MetadataField.class);

        List<MetadataValue> itemMetadata = Arrays.asList(
            metadataValue(fooMetadata),
            metadataValue(notDisplayedMetadata),
            metadataValue(barMetadata));

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(Arrays.asList(
                crisLayoutBox("box1", fooMetadata),
                crisLayoutBox("box2", barMetadata),
                crisLayoutBox("box3", bazMetadata)));

        when(crisLayoutBoxAccessService.hasAccess(any(), any(), any(), any()))
            .thenReturn(true);

        when(item.getMetadata()).thenReturn(itemMetadata);

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        List<String> boxNames = boxes.stream().map(CrisLayoutBox::getShortname).sorted().collect(Collectors.toList());

        assertThat(boxNames.size(), is(2));
        assertThat(boxNames, is(Arrays.asList("box1", "box2")));

    }

    @Test
    public void unauthorizedBoxNotReturned() throws Exception {
        Item item = Mockito.mock(Item.class);
        UUID itemUuid = UUID.randomUUID();
        int tabId = 1;

        MetadataField fooMetadata = mock(MetadataField.class);
        MetadataField barMetadata = mock(MetadataField.class);
        MetadataField bazMetadata = mock(MetadataField.class);

        List<MetadataValue> itemMetadata = Arrays.asList(
            metadataValue(fooMetadata),
            metadataValue(bazMetadata),
            metadataValue(barMetadata));

        when(itemService.find(any(Context.class), eq(itemUuid)))
            .thenReturn(item);

        when(itemService.getMetadata(item, "relationship.type"))
            .thenReturn("EntityType");

        CrisLayoutBox fooBox = crisLayoutBox("box1", fooMetadata);
        CrisLayoutBox barBox = crisLayoutBox("box2", barMetadata);
        CrisLayoutBox bazBox = crisLayoutBox("box3", bazMetadata);

        when(dao.findByEntityType(any(Context.class), eq("EntityType"), eq(tabId), any(), any()))
            .thenReturn(Arrays.asList(fooBox, barBox, bazBox));

        when(crisLayoutBoxAccessService.hasAccess(any(), any(), eq(fooBox), any()))
            .thenReturn(true);
        when(crisLayoutBoxAccessService.hasAccess(any(), any(), eq(barBox), any()))
            .thenReturn(true);
        when(crisLayoutBoxAccessService.hasAccess(any(), any(), eq(bazBox), any()))
            .thenReturn(false);

        when(item.getMetadata()).thenReturn(itemMetadata);

        List<CrisLayoutBox> boxes = crisLayoutBoxService.findByItem(context, itemUuid, tabId);

        List<String> boxNames = boxes.stream().map(CrisLayoutBox::getShortname).sorted().collect(Collectors.toList());

        assertThat(boxNames.size(), is(2));
        assertThat(boxNames, is(Arrays.asList("box1", "box2")));

    }

    @Test
    public void hasMetricsBoxContent() {

        // should return false when the box has no metrics associated
        CrisLayoutBox boxWithoutMetrics = crisLayoutMetricBox();
        assertFalse(crisLayoutBoxService.hasMetricsBoxContent(null, boxWithoutMetrics, null));

        // should return true when the box has at least one embeddable associated (stored mocked to empty)
        CrisLayoutBox boxMetric1 = crisLayoutMetricBox("metric1");
        mockStoredCrisMetrics();
        mockEmbeddableCrisMetrics("metric1");
        assertTrue(crisLayoutBoxService.hasMetricsBoxContent(null, boxMetric1, null));

        // should return true when the box has at least one stored associated (embeded mocked to empty)
        mockStoredCrisMetrics("metric1");
        mockEmbeddableCrisMetrics();
        assertTrue(crisLayoutBoxService.hasMetricsBoxContent(null, boxMetric1, null));

        // shuld return false when the box has embedded but not associated (stored mocked to empty)
        mockStoredCrisMetrics();
        mockEmbeddableCrisMetrics("metric2");
        assertFalse(crisLayoutBoxService.hasMetricsBoxContent(null, boxMetric1, null));

        // shuld return false when the box has stored but not associated (embedded mocked to empty)
        mockStoredCrisMetrics("metric2");
        mockEmbeddableCrisMetrics();
        assertFalse(crisLayoutBoxService.hasMetricsBoxContent(null, boxMetric1, null));

    }

    private CrisLayoutBox crisLayoutBox(String shortname, MetadataField metadataField) {
        CrisLayoutBox o = new CrisLayoutBox();
        o.addLayoutField(crisLayoutField(metadataField));
        o.setShortname(shortname);
        return o;
    }

    private MetadataValue metadataValue(MetadataField field) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getMetadataField()).thenReturn(field);
        return metadataValue;
    }

    private CrisLayoutField crisLayoutField(MetadataField metadataField) {
        CrisLayoutField crisLayoutField = new CrisLayoutField();
        crisLayoutField.setMetadataField(metadataField);
        return crisLayoutField;
    }

    private CrisLayoutBox crisLayoutMetricBox(String ...metricTypes) {
        CrisLayoutBox crisLayoutMetricBox = mock(CrisLayoutBox.class);
        List<CrisLayoutMetric2Box> metric2boxList = Arrays.stream(metricTypes).map(mt -> {
            CrisLayoutMetric2Box metric2box = mock(CrisLayoutMetric2Box.class);
            when(metric2box.getType()).thenReturn(mt);
            return metric2box;
        }).collect(Collectors.toList());
        when(crisLayoutMetricBox.getMetric2box()).thenReturn(metric2boxList);
        return crisLayoutMetricBox;
    }

    private List<EmbeddableCrisMetrics> mockEmbeddableCrisMetrics(String ...metricTypes) {
        List<EmbeddableCrisMetrics> metrics = Arrays.stream(metricTypes).map(mt -> {
            EmbeddableCrisMetrics metric = mock(EmbeddableCrisMetrics.class);
            when(metric.getMetricType()).thenReturn(mt);
            return metric;
        }).collect(Collectors.toList());
        when(crisItemMetricsService.getEmbeddableMetrics(any(), any())).thenReturn(metrics);
        return metrics;
    }

    private List<CrisMetrics> mockStoredCrisMetrics(String ...metricTypes) {
        List<CrisMetrics> metrics = Arrays.stream(metricTypes).map(mt -> {
            CrisMetrics metric = mock(CrisMetrics.class);
            when(metric.getMetricType()).thenReturn(mt);
            return metric;
        }).collect(Collectors.toList());
        when(crisItemMetricsService.getStoredMetrics(any(), any())).thenReturn(metrics);
        return metrics;

    }


}
