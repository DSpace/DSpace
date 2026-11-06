/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics.embeddable.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/*
*  @author Jurgen Mamani
*
*/
@RunWith(MockitoJUnitRunner.class)
public class EmbeddableDimensionsMetricProviderTest {

    @InjectMocks
    private EmbeddableDimensionsMetricProvider provider;

    @Mock
    private ItemService itemService;

    @Mock
    private Context context;

    @Mock
    private Item item;

    @Before
    public void setUp() {

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dc.identifier.doi"), Item.ANY))
            .thenReturn("doi-test");

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dc.identifier.pmid"), Item.ANY))
            .thenReturn("pmid-test");

        provider.setDetailViewEnabled(true);
        provider.setListViewEnabled(false);
        provider.setDataStyle("small_circle");
        provider.setDataLegend("hover-right");
        provider.setListDataStyle("list-small_circle");
        provider.setListDataLegend("list-hover-right");
        provider.setDoiField("dc.identifier.doi");
        provider.setPmidField("dc.identifier.pmid");

    }

    @Test
    public void testInnerHtml() {
        String innerHtml = provider.innerHtml(context, item);
        assertEquals("{"
            + "\"data-badge-enabled\":true,"
            + "\"list-data-badge-enabled\":false,"
            + "\"data-legend\":\"hover-right\","
            + "\"data-style\":\"small_circle\","
            + "\"data-doi\":\"doi-test\","
            + "\"data-pmid\":\"pmid-test\","
            + "\"data-hide-zero-citations\":false,"
            + "\"list-data-legend\":\"list-hover-right\","
            + "\"list-data-style\":\"list-small_circle\","
            + "\"list-data-doi\":\"doi-test\","
            + "\"list-data-pmid\":\"pmid-test\","
            + "\"list-data-hide-zero-citations\":false"
            + "}", innerHtml);
    }

}
