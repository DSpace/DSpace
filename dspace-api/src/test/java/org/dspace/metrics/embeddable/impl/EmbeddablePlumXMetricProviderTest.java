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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for EmbeddablePlumXMetricProvider.
 *
 * @author Alba Aliu (alba.aliu at atis.al)
 */
@RunWith(MockitoJUnitRunner.class)
public class EmbeddablePlumXMetricProviderTest {
    @Mock
    private ItemService itemService;
    @Mock
    private EmbeddablePlumXMetricProvider provider;
    @Mock
    Context context;
    @Mock
    Item item;

    @Before
    public void setUp() throws Exception {
        when(provider.innerHtml(any(), any())).thenCallRealMethod();
        when(provider.getEntityType(any())).thenCallRealMethod();
        when(provider.hasMetric(any(), any(), any())).thenCallRealMethod();
        when(provider.getItemService()).thenReturn(itemService);

        provider.personPlumXScript = "//cdn.plu.mx/widget-person.js";
        provider.publicationPlumXScript = "//cdn.plu.mx/widget-popup.js";
        provider.publicationHref = "https://plu.mx/plum/a/";
        provider.personHref = "https://plu.mx/plum/u/";
        provider.dataNumArtifacts = 5;
        provider.dataWidth = "350px";
        provider.dataPopup = "left";
        provider.listDataWidth = "350px";
        provider.listDataPopup = "left";
    }

    @Test
    public void hasMetricEmptyEntityType() {
        boolean hasMetric = provider.hasMetric(context, item, null);
        assertFalse(hasMetric);
    }

    @Test
    public void hasMetricPublicationWithoutDoi() {
        when(itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY)).thenReturn("Publication");
        when(itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY)).thenReturn(null);
        boolean hasMetric = provider.hasMetric(context, item, null);
        assertFalse(hasMetric);
    }

    @Test
    public void hasMetricPublicationWithDoi() {
        when(itemService.getMetadataFirstValue(item, "dspace",
                "entity", "type", Item.ANY)).thenReturn("Publication");
        when(itemService.getMetadataFirstValue(item, "dc",
                "identifier", "doi", Item.ANY)).thenReturn("10.1016/j.gene.2009.04.019");
        boolean hasMetric = provider.hasMetric(context, item, null);
        assertTrue(hasMetric);
    }

    @Test
    public void hasMetricPublicationWithOrcid() {
        when(itemService.getMetadataFirstValue(item, "dspace",
                "entity", "type", Item.ANY)).thenReturn("Person");
        when(itemService.getMetadataFirstValue(item, "person",
                "identifier", "orcid", Item.ANY)).thenReturn("0000-0002-9029-1854");
        boolean hasMetric = provider.hasMetric(context, item, null);
        assertTrue(hasMetric);
    }

    @Test
    public void hasMetricPublicationWithoutOrcid() {
        when(itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY)).thenReturn("Person");
        when(itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY)).thenReturn(null);
        boolean hasMetric = provider.hasMetric(context, item, null);
        assertFalse(hasMetric);
    }

    @Test
    public void innerHtmlForPersonItem() {
        when(itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY)).thenReturn("Person");
        when(itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY))
                .thenReturn("0000-0002-9029-1854");
        String template = provider.innerHtml(context, item);

        assertEquals("{\"data-person-badge-enabled\":false,\"list-data-person-badge-enabled\":false," +
                "\"data-publication-badge-enabled\":false,\"list-data-publication-badge-enabled\":false," +
                "\"type\":\"Person\",\"list-type\":\"Person\",\"placeholder\":\"\",\"list-placeholder\":\"\"," +
                "\"src\":\"//cdn.plu.mx/widget-person.js\",\"href\":\"https://plu.mx/plum/u/?orcid=0000-0002-9029-1854\"," +
                "\"list-src\":\"//cdn.plu.mx/widget-person.js\"," +
                "\"list-href\":\"https://plu.mx/plum/u/?orcid=0000-0002-9029-1854\",\"data-no-name\":false," +
                "\"data-num-artifacts\":5,\"data-width\":\"350px\",\"data-no-description\":false," +
                "\"data-no-stats\":false,\"data-no-thumbnail\":false,\"data-no-artifacts\":false," +
                "\"data-popup\":\"left\",\"data-hide-when-empty\":false,\"data-hide-usage\":false," +
                "\"data-hide-captures\":false,\"data-hide-mentions\":false,\"data-hide-socialmedia\":false," +
                "\"data-hide-citations\":false,\"data-pass-hidden-categories\":false,\"data-detail-same-page\":false," +
                "\"list-data-no-name\":false,\"list-data-num-artifacts\":0,\"list-data-width\":\"350px\"," +
                "\"list-data-no-description\":false,\"list-data-no-stats\":false,\"list-data-no-thumbnail\":false," +
                "\"list-data-no-artifacts\":false,\"list-data-popup\":\"left\",\"list-data-hide-when-empty\":false," +
                "\"list-data-hide-usage\":false,\"list-data-hide-captures\":false,\"list-data-hide-mentions\":false," +
                "\"list-data-hide-socialmedia\":false,\"list-data-hide-citations\":false," +
                "\"list-data-pass-hidden-categories\":false,\"list-data-detail-same-page\":false}", template);
    }

    @Test
    public void innerHtmlForPublicationItem() {
        when(itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY)).thenReturn("Publication");
        when(itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY))
                .thenReturn("10.1016/j.gene.2009.04.019");
        String template = provider.innerHtml(context, item);

        assertEquals("{\"data-person-badge-enabled\":false,\"list-data-person-badge-enabled\":false," +
                "\"data-publication-badge-enabled\":false,\"list-data-publication-badge-enabled\":false," +
                "\"type\":\"Publication\",\"list-type\":\"Publication\",\"placeholder\":\"\"," +
                "\"list-placeholder\":\"\",\"src\":\"//cdn.plu.mx/widget-popup.js\"," +
                "\"href\":\"https://plu.mx/plum/a/?doi=10.1016/j.gene.2009.04.019\"," +
                "\"list-src\":\"//cdn.plu.mx/widget-popup.js\"," +
                "\"list-href\":\"https://plu.mx/plum/a/?doi=10.1016/j.gene.2009.04.019\",\"data-no-name\":false," +
                "\"data-num-artifacts\":5,\"data-width\":\"350px\",\"data-no-description\":false," +
                "\"data-no-stats\":false,\"data-no-thumbnail\":false,\"data-no-artifacts\":false," +
                "\"data-popup\":\"left\",\"data-hide-when-empty\":false,\"data-hide-usage\":false," +
                "\"data-hide-captures\":false,\"data-hide-mentions\":false,\"data-hide-socialmedia\":false," +
                "\"data-hide-citations\":false,\"data-pass-hidden-categories\":false,\"data-detail-same-page\":false," +
                "\"list-data-no-name\":false,\"list-data-num-artifacts\":0,\"list-data-width\":\"350px\"," +
                "\"list-data-no-description\":false,\"list-data-no-stats\":false,\"list-data-no-thumbnail\":false," +
                "\"list-data-no-artifacts\":false,\"list-data-popup\":\"left\",\"list-data-hide-when-empty\":false," +
                "\"list-data-hide-usage\":false,\"list-data-hide-captures\":false,\"list-data-hide-mentions\":false," +
                "\"list-data-hide-socialmedia\":false,\"list-data-hide-citations\":false," +
                "\"list-data-pass-hidden-categories\":false,\"list-data-detail-same-page\":false}", template);
    }
}
