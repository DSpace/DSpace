/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.metrics.embeddable.EmbeddableMetricProvider;
import org.dspace.metrics.embeddable.impl.AbstractEmbeddableMetricProvider;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.dspace.metricsSecurity.BoxMetricsLayoutConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CrisItemMetricsServiceImpl.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */

@RunWith(MockitoJUnitRunner.class)
public class CrisItemMetricsServiceImplTest {

    @Mock
    private ItemService itemService;

    @Mock
    private IndexingService indexingService;

    @Mock
    private CrisMetricsService crisMetricsService;

    @Mock
    private EmbeddableMetricProvider provider1;

    @Mock
    private EmbeddableMetricProvider provider2;

    private List<EmbeddableMetricProvider> providers;

    EmbeddableCrisMetrics embeddable1;

    @Mock
    Context context;

    @Mock
    Item item;

    @Mock
    CrisItemMetricsServiceImpl crisItemMetricsService;

    @Mock
    private BoxMetricsLayoutConfigurationService boxMetricsLayoutConfigurationService;

    @Before
    public void setUp() throws Exception {
        embeddable1 = new EmbeddableCrisMetrics();

        this.providers = new ArrayList<>();
        providers.add(provider1);
        providers.add(provider2);

        when(itemService.find(context, item.getID())).thenReturn(item);

        crisItemMetricsService = mock(CrisItemMetricsServiceImpl.class);
        crisItemMetricsService.providers = providers;
        crisItemMetricsService.itemService = itemService;
        crisItemMetricsService.boxMetricsLayoutConfigurationService = boxMetricsLayoutConfigurationService;
        crisItemMetricsService.crisMetricsService = crisMetricsService;
    }

    @Test
    public void getMetrics() {

        List<CrisMetrics> stored = new ArrayList<CrisMetrics>();
        stored.add(new CrisMetrics());
        List<EmbeddableCrisMetrics> embeddable = new ArrayList<EmbeddableCrisMetrics>();
        embeddable.add(new EmbeddableCrisMetrics());

        // should combine stored and embeddable
        when(crisItemMetricsService.getStoredMetrics(any(), any())).thenReturn(stored);
        when(crisItemMetricsService.getEmbeddableMetrics(any(), any(), any())).thenReturn(embeddable);
        when(crisItemMetricsService.getMetrics(any(), any())).thenCallRealMethod();

        assertEquals(crisItemMetricsService.getMetrics(null, null).size(), 2);
    }

    @Test
    public void getStoredMetrics() {

        // should call findMetricsByItemUUID
        when(crisItemMetricsService.getStoredMetrics(context, item.getID())).thenCallRealMethod();
        when(crisItemMetricsService.findMetricsByItemUUID(context, item.getID())).thenReturn(null);

        crisItemMetricsService.getStoredMetrics(context, item.getID());
        verify(crisItemMetricsService, times(1)).findMetricsByItemUUID(context, item.getID());
    }

    @Test
    public void getEmbeddableMetrics() throws SQLException {

        // should call provide on each provider and add every present metric

        when(provider1.provide(context, item, null)).thenReturn(Optional.of(embeddable1));
        when(provider2.provide(context, item, null)).thenReturn(Optional.empty());
        when(crisItemMetricsService.getEmbeddableMetrics(context, item.getID(), null)).thenCallRealMethod();
        when(crisItemMetricsService.checkPermissionsOfMetricsByBox(any(), any(), any())).thenReturn(true);
        List<EmbeddableCrisMetrics> result = crisItemMetricsService.getEmbeddableMetrics(context, item.getID(), null);

        verify(provider1, times(1)).provide(context, item, null);
        verify(provider2, times(1)).provide(context, item, null);

        assertEquals(result.size(), 1);

    }

    @Test
    public void getEmbeddableById() throws SQLException {
        // should call provide on the provider wich support the id

        final String metricId = "metricId";
        when(provider1.support(metricId)).thenReturn(false);
        when(provider2.support(metricId)).thenReturn(true);
        when(provider2.provide(context, metricId)).thenReturn(Optional.of(embeddable1));
        when(crisItemMetricsService.getEmbeddableById(context,metricId)).thenCallRealMethod();
        when(crisItemMetricsService.checkPermissionsOfMetricsByBox(any(), any(), any())).thenReturn(true);
        Optional<EmbeddableCrisMetrics> result = crisItemMetricsService.getEmbeddableById(context, metricId);

        verify(provider1, times(1)).support(metricId);
        verify(provider2, times(1)).support(metricId);

        verify(provider1, times(0)).provide(context, metricId);
        verify(provider2, times(1)).provide(context, metricId);

        assertEquals(result.get(), embeddable1);
    }

    @Test
    public void find() throws SQLException {
        List<CrisMetrics> storedList = new ArrayList<CrisMetrics>();
        CrisMetrics stored = new CrisMetrics();
        storedList.add(stored);
        List<EmbeddableCrisMetrics> embeddableList = new ArrayList<EmbeddableCrisMetrics>();
        EmbeddableCrisMetrics embeddable = new EmbeddableCrisMetrics();
        embeddableList.add(embeddable);

        when(crisItemMetricsService.find(eq(context), any())).thenCallRealMethod();
        when(crisItemMetricsService.isEmbeddableMetricId(any())).thenCallRealMethod();
        when(crisItemMetricsService.checkPermissionsOfMetricsByBox(any(), any(), any())).thenReturn(true);
        when(crisItemMetricsService.itemFromMetricId(any(), any())).thenReturn(item);

        // should return getEmbeddableByID if is embeddable metric
        String metricId =
                "itemUUID" + AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR + "metricType";
        when(crisItemMetricsService.getEmbeddableById(eq(context), eq(metricId))).thenReturn(Optional.of(embeddable));
        assertEquals(crisItemMetricsService.find(context, metricId), embeddable);

        // should return crisMetricService.find otherwise (stored metric)
        Integer id = 1;
        when(crisMetricsService.find(eq(context), eq(id))).thenReturn(stored);
        assertEquals(crisItemMetricsService.find(context,
                CrisItemMetricsService.STORED_METRIC_ID_PREFIX + String.valueOf(id)), stored);

    }


}
