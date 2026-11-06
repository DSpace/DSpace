/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for AbstractEmbeddableMetricProvider.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */

@RunWith(MockitoJUnitRunner.class)
public class AbstractEmbeddableMetricProviderTest {

    protected static class EmbeddableMetricProviderMockImpl extends AbstractEmbeddableMetricProvider {

        @Override
        public String innerHtml(Context context, Item item) {
            return "innerHtml";
        }

        @Override
        public String getMetricType() {
            return "metricType";
        }

    }

    private EmbeddableMetricProviderMockImpl provider;

    @Mock
    private ItemService itemService;

    @Mock
    private Filter filterService;

    @Mock
    private Item item;

    private UUID itemUUID;

    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {
        provider = new EmbeddableMetricProviderMockImpl();
        provider.setFilterService(filterService);
        provider.setItemService(itemService);

        itemUUID = UUID.randomUUID();
        when(item.getID()).thenReturn(itemUUID);
    }

    @Test
    public void hasMetric() throws LogicalStatementException {

        // should return false if provider is not enabled
        provider.setEnabled(false);
        assertFalse(provider.hasMetric(context, item, null));
        verify(filterService, times(0)).getResult(context, item);

        // should return getResult of filter if provider is enabled
        provider.setEnabled(true);

        when(filterService.getResult(eq(context), eq(item))).thenReturn(false);
        assertFalse(provider.hasMetric(context, item, null));
        when(filterService.getResult(eq(context), eq(item))).thenReturn(true);
        assertTrue(provider.hasMetric(context, item, null));

    }

    @Test
    public void provide() throws LogicalStatementException {

        // should return empty if hasMetric is false
        provider.setEnabled(false);
        assertTrue(provider.provide(context, item, null).isEmpty());

        // should return EmbeddableCrisMetrics with getId, metricType and innerHtml called
        provider.setEnabled(true);
        when(filterService.getResult(eq(context), eq(item))).thenReturn(true);

        Optional<EmbeddableCrisMetrics> metric = provider.provide(context, item, null);
        assertTrue(metric.isPresent());
        assertEquals(metric.get().getEmbeddableId(), provider.getId(context, item));
        assertEquals(metric.get().getRemark(), provider.innerHtml(context, item));
        assertEquals(metric.get().getMetricType(), provider.getMetricType());
    }

    @Test
    public void support() {
        // should return true if metricId contains metricyType
        String metricId =
                itemUUID.toString() + AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR + provider.getMetricType();
        assertTrue(provider.support(metricId));

        metricId =
                itemUUID.toString() + AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR + "metricType2";
        assertFalse(provider.support(metricId));
    }

    @Test
    public void getId() {
        // should join itemUuid and metrictype with the separator
        String id = provider.getId(context, item);
        assertEquals(id,
                itemUUID.toString() + AbstractEmbeddableMetricProvider.DYNAMIC_ID_SEPARATOR + provider.getMetricType());
    }


}
