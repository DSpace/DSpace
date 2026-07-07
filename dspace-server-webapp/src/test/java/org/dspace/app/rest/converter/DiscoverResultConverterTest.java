/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.util.Collections;
import java.util.List;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link DiscoverResultConverter}, focused on the spellcheck support.
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoverResultConverterTest {

    @InjectMocks
    private DiscoverResultConverter discoverResultConverter;

    @Mock
    private DiscoverFacetsConverter facetConverter;

    @Mock
    private SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter;

    @Mock
    private ConverterService converterService;

    @Mock
    private Context context;

    @Mock
    private DiscoveryConfiguration discoveryConfiguration;

    private DiscoverResult discoverResult;
    private Pageable page;

    @Before
    public void setUp() {
        discoverResult = new DiscoverResult();
        discoverResult.setTotalSearchResults(0);
        page = PageRequest.of(0, 10);

        doNothing().when(facetConverter)
                   .addFacetValues(any(), any(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // spellcheck: suggestion presente
    // -----------------------------------------------------------------------

    @Test
    public void testSpellCheckSuggestionIsSetWhenPresent() {
        discoverResult.setSpellCheckSuggestions(List.of("java"));

        SearchResultsRest result = convert("jav");

        assertEquals("java", result.getSpellCheckSuggestions());
    }

    @Test
    public void testSpellCheckSuggestionIsTrimmed() {
        discoverResult.setSpellCheckSuggestions(List.of("  java  "));

        SearchResultsRest result = convert("jav");

        assertEquals("java", result.getSpellCheckSuggestions());
    }

    // -----------------------------------------------------------------------
    // spellcheck: suggestion assente / vuota / blank
    // -----------------------------------------------------------------------

    @Test
    public void testSpellCheckSuggestionIsNullWhenNotProvided() {
        // nessuna suggestion impostata sul DiscoverResult
        SearchResultsRest result = convert("anyquery");

        assertNull(result.getSpellCheckSuggestions());
    }

    @Test
    public void testSpellCheckSuggestionIsNullWhenEmpty() {
        discoverResult.setSpellCheckSuggestions(List.of(""));

        SearchResultsRest result = convert("anyquery");

        assertNull(result.getSpellCheckSuggestions());
    }

    @Test
    public void testSpellCheckSuggestionIsNullWhenBlank() {
        discoverResult.setSpellCheckSuggestions(List.of("   "));

        SearchResultsRest result = convert("anyquery");

        assertNull(result.getSpellCheckSuggestions());
    }

    // -----------------------------------------------------------------------
    // helper
    // -----------------------------------------------------------------------

    private SearchResultsRest convert(String query) {
        return discoverResultConverter.convert(
                context,
                query,
                Collections.emptyList(),
                "default",
                null,
                Collections.<SearchFilter>emptyList(),
                page,
                discoverResult,
                discoveryConfiguration,
                Projection.DEFAULT);
    }
}
