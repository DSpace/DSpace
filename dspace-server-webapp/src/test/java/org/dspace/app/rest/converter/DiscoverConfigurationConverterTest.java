/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.LinkedList;

import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class' purpose is to test the DiscoverConfigurationConverter
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoverConfigurationConverterTest {

    SearchConfigurationRest searchConfigurationRest;

    @InjectMocks
    private DiscoverConfigurationConverter discoverConfigurationConverter;

    @Mock
    private DiscoverySortConfiguration discoverySortConfiguration;

    @Mock
    private DiscoveryConfiguration discoveryConfiguration;

    @Before
    public void setUp() throws Exception {
    }

    public void populateDiscoveryConfigurationWithEmptyList() {
        discoveryConfiguration.setSearchFilters(new LinkedList<DiscoverySearchFilter>());
        discoveryConfiguration.setSearchSortConfiguration(new DiscoverySortConfiguration());
    }

    @Test
    public void testReturnType() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertTrue(searchConfigurationRest.getFilters().isEmpty());
        assertEquals(SearchConfigurationRest.class, searchConfigurationRest.getClass());
    }

    @Test
    public void testConvertWithNullParamter() throws Exception {
        assertNotNull(discoverConfigurationConverter.convert(null, Projection.DEFAULT));
    }

    @Test
    public void testNoSearchSortConfigurationReturnObjectNotNull() throws Exception {
        discoveryConfiguration.setSearchFilters(new LinkedList<>());
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertTrue(discoveryConfiguration.getSearchFilters().isEmpty());
        assertTrue(searchConfigurationRest.getFilters().isEmpty());
        assertNotNull(searchConfigurationRest);
    }

    @Test
    public void testNoSearchFilterReturnObjectNotNull() throws Exception {
        discoveryConfiguration.setSearchSortConfiguration(new DiscoverySortConfiguration());
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertTrue(searchConfigurationRest.getFilters().isEmpty());
        assertNotNull(searchConfigurationRest);
    }

    //Checks that the convert is still done properly without error even if the discoveryConfiguration's attributes
    // are null
    @Test
    public void testNoSearchSortConfigurationAndNoSearchFilterReturnObjectNotNull() throws Exception {
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertNotNull(searchConfigurationRest);
    }

    @Test
    public void testCorrectSortOptionsAfterConvert() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();

        DiscoverySortFieldConfiguration discoverySortFieldConfiguration = new DiscoverySortFieldConfiguration();
        discoverySortFieldConfiguration.setMetadataField("title");
        discoverySortFieldConfiguration.setType("text");
        DiscoverySortFieldConfiguration discoverySortFieldConfiguration1 = new DiscoverySortFieldConfiguration();
        discoverySortFieldConfiguration1.setMetadataField("author");
        discoverySortFieldConfiguration1.setType("text");
        LinkedList<DiscoverySortFieldConfiguration> mockedList = new LinkedList<>();
        mockedList.add(discoverySortFieldConfiguration);
        mockedList.add(discoverySortFieldConfiguration1);

        when(discoveryConfiguration.getSearchSortConfiguration()).thenReturn(discoverySortConfiguration);
        when(discoverySortConfiguration.getSortFields()).thenReturn(mockedList);

        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);

        int counter = 0;
        for (SearchConfigurationRest.SortOption sortOption : searchConfigurationRest.getSortOptions()) {
            assertEquals(mockedList.get(counter).getMetadataField(), sortOption.getName());
            assertEquals(mockedList.get(counter).getType(), sortOption.getActualName());
            counter++;
        }

        assertFalse(searchConfigurationRest.getSortOptions().isEmpty());
    }

    @Test
    public void testEmptySortOptionsAfterConvertWithConfigurationWithEmptySortFields() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertEquals(0, searchConfigurationRest.getSortOptions().size());

    }

    @Test
    public void testEmptySortOptionsAfterConvertWithConfigurationWithNullSortFields() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();
        when(discoveryConfiguration.getSearchSortConfiguration()).thenReturn(null);
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);

        assertEquals(0, searchConfigurationRest.getSortOptions().size());
    }

    @Test
    public void testCorrectSearchFiltersAfterConvert() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();

        LinkedList<DiscoverySearchFilter> mockedList = new LinkedList();
        DiscoverySearchFilter discoverySearchFilter = new DiscoverySearchFilter();
        discoverySearchFilter.setIndexFieldName("title");
        DiscoverySearchFilter discoverySearchFilter1 = new DiscoverySearchFilter();
        discoverySearchFilter1.setIndexFieldName("title2");

        mockedList.add(discoverySearchFilter);
        mockedList.add(discoverySearchFilter1);
        when(discoveryConfiguration.getSearchFilters()).thenReturn(mockedList);

        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);

        int counter = 0;
        for (SearchConfigurationRest.Filter filter : searchConfigurationRest.getFilters()) {
            DiscoverySearchFilter searchFilter = mockedList.get(counter);
            assertEquals(searchFilter.getIndexFieldName(), filter.getFilter());
            //TODO checkoperators
            SearchConfigurationRest.Filter.Operator operator = new SearchConfigurationRest.Filter.Operator("testing");
            filter.addOperator(operator);
            assertEquals(filter.getOperators().get(filter.getOperators().size() - 1), operator);
            counter++;
        }
        assertTrue(!(searchConfigurationRest.getFilters().isEmpty()));
    }

    @Test
    public void testEmptySearchFilterAfterConvertWithConfigurationWithEmptySearchFilters() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);
        assertEquals(0, searchConfigurationRest.getFilters().size());
    }

    @Test
    public void testEmptySearchFiltersAfterConvertWithConfigurationWithNullSearchFilters() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();

        when(discoveryConfiguration.getSearchFilters()).thenReturn(null);
        searchConfigurationRest = discoverConfigurationConverter.convert(discoveryConfiguration, Projection.DEFAULT);

        assertEquals(0, searchConfigurationRest.getFilters().size());
    }
}
