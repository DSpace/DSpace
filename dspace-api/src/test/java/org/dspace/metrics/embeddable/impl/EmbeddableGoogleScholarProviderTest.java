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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
public class EmbeddableGoogleScholarProviderTest {

    @Mock
    private EmbeddableGoogleScholarProvider provider;

    @Mock
    private ItemService itemService;

    @Mock
    Item item;

    @Mock
    Context context;

    private String field = "field";

    private String fallbackField = "fallbackField";

    private List<MetadataValue> fieldValueList;

    private List<MetadataValue> fallbackValueList;

    private List<MetadataValue> emptyMetadataValues;

    @Before
    public void setUp() throws Exception {
        when(provider.getItemService()).thenReturn(itemService);
        provider.field = field;
        provider.fallbackField = fallbackField;

        fieldValueList = new ArrayList<MetadataValue>();
        MetadataValue value = mock(MetadataValue.class);
        when(value.getValue()).thenReturn("value");
        fieldValueList.add(value);

        fallbackValueList = new ArrayList<MetadataValue>();
        MetadataValue fallbackValue = mock(MetadataValue.class);
        when(fallbackValue.getValue()).thenReturn("fallbackValue");
        fallbackValueList.add(fallbackValue);

        emptyMetadataValues = new ArrayList<MetadataValue>();
    }

    @Test
    public void innerHtml() {

        when(provider.innerHtml(any(), any())).thenCallRealMethod();

        when(provider.getRelationshipType(item)).thenReturn("Publication");
        when(provider.calculateSearchText(item)).thenReturn("calculatedText");
        when(provider.getTemplate("Publication")).thenReturn("TextSearch={{searchText}}");

        String innerHtml = this.provider.innerHtml(context, item);

        verify(provider, times(1)).getRelationshipType(item);
        // should call calculatedSearchText with the item
        verify(provider, times(1)).calculateSearchText(item);
        // should call getTemplate with the relationshipItem
        verify(provider, times(1)).getTemplate("Publication");

        // should replace searchText with the calculatedSearchText
        assertEquals(innerHtml, "TextSearch=calculatedText");
    }

    @Test
    public void calculateSearchText() {
        when(provider.calculateSearchText(any())).thenCallRealMethod();

        // should return the value of field if present
        when(itemService.getMetadataByMetadataString(any(), eq(field))).thenReturn(fieldValueList);
        assertEquals(provider.calculateSearchText(item), "value");

        // should return fallbackField value if field value is not present
        when(itemService.getMetadataByMetadataString(any(), eq(field))).thenReturn(emptyMetadataValues);
        when(itemService.getMetadataByMetadataString(any(), eq(fallbackField))).thenReturn(fallbackValueList);

        assertEquals(provider.calculateSearchText(item), "fallbackValue");
    }

    @Test
    public void getTemplate() {
        when(provider.getTemplate(any())).thenCallRealMethod();
        // should return Publication Template when relationshipType is Publication
        assertEquals(provider.getTemplate("Publication"), provider.PUBLICATION_TEMPLATE);
        // should return Person Template when relationshipType is Person
        assertEquals(provider.getTemplate("Person"), provider.PERSON_TEMPLATE);
    }

}
