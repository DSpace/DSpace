/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for SolrSuggestService, specifically the isAllowedDictionary logic.
 *
 * The allowed-dictionaries config behavior should be:
 * - Empty/null list: no dictionaries are allowed (deny all)
 * - Explicit list: only those named dictionaries are allowed
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrSuggestServiceTest {

    @InjectMocks
    private SolrSuggestService solrSuggestService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private SolrSearchCore solrSearchCore;

    private static final String CONFIG_KEY = "discovery.suggest.allowed-dictionaries";

    @Before
    public void setUp() {
    }

    /**
     * When the allowed-dictionaries config is null (not set), no dictionaries should be allowed.
     */
    @Test
    public void nullConfigShouldDenyAll() {
        when(configurationService.getArrayProperty(CONFIG_KEY)).thenReturn(null);

        assertThat(solrSuggestService.isAllowedDictionary("subject"), is(false));
        assertThat(solrSuggestService.isAllowedDictionary("authors"), is(false));
        assertThat(solrSuggestService.isAllowedDictionary("anything"), is(false));
    }

    /**
     * When the allowed-dictionaries config is empty, no dictionaries should be allowed.
     */
    @Test
    public void emptyConfigShouldDenyAll() {
        when(configurationService.getArrayProperty(CONFIG_KEY)).thenReturn(new String[]{});

        assertThat(solrSuggestService.isAllowedDictionary("subject"), is(false));
        assertThat(solrSuggestService.isAllowedDictionary("authors"), is(false));
        assertThat(solrSuggestService.isAllowedDictionary("anything"), is(false));
    }

    /**
     * When the allowed-dictionaries config contains explicit names,
     * only those names should be allowed.
     */
    @Test
    public void explicitConfigShouldAllowOnlyListed() {
        when(configurationService.getArrayProperty(CONFIG_KEY))
                .thenReturn(new String[]{"subject", "countries_file"});

        assertThat(solrSuggestService.isAllowedDictionary("subject"), is(true));
        assertThat(solrSuggestService.isAllowedDictionary("countries_file"), is(true));
        assertThat(solrSuggestService.isAllowedDictionary("authors"), is(false));
        assertThat(solrSuggestService.isAllowedDictionary("not_in_list"), is(false));
    }
}
