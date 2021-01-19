/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
public class EmbeddableAltmetricsProviderTest {

    @Mock
    private EmbeddableAltmetricsProvider provider;

    @Mock
    private ItemService itemService;

    @Mock
    Item item;

    @Mock
    Context context;

    private List<MetadataValue> fieldValueList;

    @Before
    public void setUp() throws Exception {
        when(provider.getItemService()).thenReturn(itemService);

        provider.doiDataAttr = "doiAttr";
        provider.doiField = "doiField";
        provider.pmidDataAttr = "pmidAttr";
        provider.pmidField = "pmidField";
        provider.popover = "popover";
        provider.badgeType = "badgeType";

        fieldValueList = new ArrayList<MetadataValue>();
        MetadataValue value = mock(MetadataValue.class);
        when(value.getValue()).thenReturn("value");
        fieldValueList.add(value);
    }

    @Test
    public void innerHtml() {
        when(provider.innerHtml(any(), any())).thenCallRealMethod();

        // should replace doiAttr whit the calculated attribute
        // should replace pmidAttr whit the calculated attribute
        when(provider.calculateAttribute(eq(item), eq("doiField"), eq("doiAttr")))
        .thenReturn("calculatedDoi");
        when(provider.calculateAttribute(eq(item), eq("pmidField"), eq("pmidAttr")))
        .thenReturn("calculatedPmid");
        when(provider.getTemplate()).thenReturn("template {{doiAttr}} {{pmidAttr}} {{popover}} {{badgeType}}");

        String innerHtml = provider.innerHtml(context, item);

        assertEquals(innerHtml, "template calculatedDoi calculatedPmid popover badgeType");

    }

    @Test
    public void calculateAttribute() {
        // should concat attr with first metadata value of field
        when(provider.calculateAttribute(eq(item), eq("field"), eq("attr"))).thenCallRealMethod();
        when(itemService.getMetadataByMetadataString(any(), eq("field"))).thenReturn(fieldValueList);
        assertEquals(provider.calculateAttribute(item, "field", "attr"), "attr=value");
    }


}
