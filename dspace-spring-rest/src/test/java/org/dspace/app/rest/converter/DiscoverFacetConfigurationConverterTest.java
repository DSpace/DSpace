/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;


import org.dspace.app.rest.converter.DiscoverConfigurationConverter;
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.hateoas.FacetConfigurationResource;
import org.dspace.discovery.configuration.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by raf on 26/09/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoverFacetConfigurationConverterTest{

    FacetConfigurationRest facetConfigurationRest;

    @InjectMocks
    private DiscoverFacetConfigurationConverter discoverFacetConfigurationConverter;

    @Mock
    private DiscoveryConfiguration discoveryConfiguration;


    public void populateDiscoveryConfigurationWithEmptyList(){
        discoveryConfiguration.setSidebarFacets(new LinkedList<DiscoverySearchFilterFacet>());
    }

    @Test
    public void testReturnType() throws Exception{
        populateDiscoveryConfigurationWithEmptyList();
        facetConfigurationRest = discoverFacetConfigurationConverter.convert(discoveryConfiguration);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
        assertEquals(FacetConfigurationRest.class, facetConfigurationRest.getClass());
    }
    @Test
    public void testConvertWithNullParamter() throws Exception{
        facetConfigurationRest = discoverFacetConfigurationConverter.convert(null);
        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }

    @Test
    public void testConvertWithConfigurationContainingSidebarFacetsFacetConfigurationRestContainsCorrectSidebarFacet() throws Exception{
        LinkedList<DiscoverySearchFilterFacet> discoverySearchFilterFacets = new LinkedList<>();
        DiscoverySearchFilterFacet discoverySearchFilterFacet = new DiscoverySearchFilterFacet();
        discoverySearchFilterFacet.setIndexFieldName("Testing");
        discoverySearchFilterFacet.setType("test");
        discoverySearchFilterFacets.add(discoverySearchFilterFacet);

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(discoverySearchFilterFacets);

        facetConfigurationRest = discoverFacetConfigurationConverter.convert(discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(!facetConfigurationRest.getSidebarFacets().isEmpty());
        assertEquals(discoverySearchFilterFacet.getIndexFieldName(), facetConfigurationRest.getSidebarFacets().get(0).getName());
        assertEquals(discoverySearchFilterFacet.getType(), facetConfigurationRest.getSidebarFacets().get(0).getType());
    }

    @Test
    public void testConvertWithConfigurationContainingEmptySidebarFacetListFacetConfigurationRestSidebarFacetsIsEmpty() throws Exception{

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(new LinkedList<DiscoverySearchFilterFacet>());

        facetConfigurationRest = discoverFacetConfigurationConverter.convert(discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }

    @Test
    public void testConvertWithConfigurationContainingNullSidebarFacetListFacetConfigurationRestSidebarFacetsIsEmpty() throws Exception{

        when(discoveryConfiguration.getSidebarFacets()).thenReturn(null);

        facetConfigurationRest = discoverFacetConfigurationConverter.convert(discoveryConfiguration);

        assertNotNull(facetConfigurationRest);
        assertTrue(facetConfigurationRest.getSidebarFacets().isEmpty());
    }
}
