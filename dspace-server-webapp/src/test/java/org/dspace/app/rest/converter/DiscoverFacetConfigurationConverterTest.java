/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.LinkedList;

import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class has the purpose to test the DiscoverFacetConfigurationConverter
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoverFacetConfigurationConverterTest {

    FacetConfigurationRest facetConfigurationRest;

    @InjectMocks
    private DiscoverFacetConfigurationConverter discoverFacetConfigurationConverter;

    @Mock
    private DiscoveryConfiguration discoveryConfiguration;

    private String configurationName = "default";
    private String scopeObject = "ba9e1c83-8144-4e9c-9d58-bb97be573b46";

    public void populateDiscoveryConfigurationWithEmptyList() {
        discoveryConfiguration.setSidebarFacets(new LinkedList<DiscoverySearchFilterFacet>());
    }

    @Test
    public void testReturnType() throws Exception {
        populateDiscoveryConfigurationWithEmptyList();
        facetConfigurationRest = discoverFacetConfigurationConverter
            .convert(configurationName, scopeObject, discoveryConfiguration);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
        assertEquals(FacetConfigurationRest.class, facetConfigurationRest.getClass());
    }

    @Test
    public void testConvertWithNullParamter() throws Exception {
        facetConfigurationRest = discoverFacetConfigurationConverter.convert(configurationName, scopeObject, null);
        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }

    @Test
    public void testConvertWithConfigurationContainingSidebarFacetsFacetConfigurationRestContainsCorrectSidebarFacet()
        throws Exception {
        LinkedList<DiscoverySearchFilterFacet> discoverySearchFilterFacets = new LinkedList<>();
        DiscoverySearchFilterFacet discoverySearchFilterFacet = new DiscoverySearchFilterFacet();
        discoverySearchFilterFacet.setIndexFieldName("Testing");
        discoverySearchFilterFacet.setType("test");
        discoverySearchFilterFacets.add(discoverySearchFilterFacet);

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(discoverySearchFilterFacets);

        facetConfigurationRest = discoverFacetConfigurationConverter
            .convert(configurationName, scopeObject, discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(!facetConfigurationRest.getSidebarFacets().isEmpty());
        assertEquals(discoverySearchFilterFacet.getIndexFieldName(),
                     facetConfigurationRest.getSidebarFacets().get(0).getName());
        assertEquals(discoverySearchFilterFacet.getType(),
                     facetConfigurationRest.getSidebarFacets().get(0).getFacetType());
    }

    @Test
    public void testConvertWithConfigurationContainingEmptySidebarFacetListFacetConfigurationRestSidebarFacetsIsEmpty()
        throws Exception {

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(new LinkedList<DiscoverySearchFilterFacet>());

        facetConfigurationRest = discoverFacetConfigurationConverter
            .convert(configurationName, scopeObject, discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }

    @Test
    public void testConvertWithConfigurationContainingNullSidebarFacetListFacetConfigurationRestSidebarFacetsIsEmpty()
        throws Exception {

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(null);

        facetConfigurationRest = discoverFacetConfigurationConverter
            .convert(configurationName, scopeObject, discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }
}
