/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openalex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPublicationLoaderTest {

    private OpenAlexPublicationLoader loader;

    @Mock
    private Item researcher;

    private ItemService itemService;

    @Before
    public void setUp() {
        itemService = Mockito.mock(ItemService.class);
        loader = new OpenAlexPublicationLoader();
        loader.setItemService(itemService);
    }

    @Test
    public void testSearchMetadataValues_NoMetadata_ReturnsEmptyList() {
        // Given: No metadata values exist
        when(itemService.getMetadata(any(Item.class), anyString())).thenReturn(null);
        loader.setNames(Collections.singletonList("dc.contributor.author"));

        // When
        List<String> result = loader.searchMetadataValues(researcher);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchMetadataValues_WithDcIdentifierOther_ReturnsFilterQuery() {

        loader.setNames(Arrays.asList("dc.identifier.openalex", "dc.contributor.author"));
        when(itemService.getMetadata(researcher, "dc.identifier.openalex")).thenReturn("ID123");

        // When
        List<String> result = loader.searchMetadataValues(researcher);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ID123", result.get(0));
    }

    @Test
    public void testSearchMetadataValues_NoDcIdentifierOther_ReturnsAuthorIds() {

        loader.setNames(Collections.singletonList("dc.contributor.author"));
        when(itemService.getMetadata(researcher, "dc.contributor.author")).thenReturn("Author Name");

        // When
        List<String> result = loader.searchMetadataValues(researcher);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("search_by_author=Author Name", result.get(0));
    }

    @Test
    public void testSearchMetadataValues_MixedNullValues_IgnoresNulls() {

        loader.setNames(Arrays.asList("dc.identifier.openalex", "dc.contributor.author", "dc.title"));
        when(itemService.getMetadata(researcher, "dc.identifier.openalex")).thenReturn(null);
        when(itemService.getMetadata(researcher, "dc.contributor.author")).thenReturn("Author Name");
        when(itemService.getMetadata(researcher, "dc.title")).thenReturn("Title");

        // When
        List<String> result = loader.searchMetadataValues(researcher);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("search_by_author=Author Name", result.get(0));
        assertEquals("search_by_author=Title", result.get(1));
    }
}

