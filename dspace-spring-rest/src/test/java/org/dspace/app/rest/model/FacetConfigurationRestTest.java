/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class FacetConfigurationRestTest {

    FacetConfigurationRest facetConfigurationRest;

    @Before
    public void setUp() throws Exception{
        facetConfigurationRest = new FacetConfigurationRest();
    }

    @Test
    public void testSidebarFacetsNotNullAfterConstructor(){
        assertNotNull(facetConfigurationRest.getSidebarFacets());
    }

    @Test
    public void testAddSidebarFacetsContainsCorrectSidebarFacet(){
        FacetConfigurationRest.SidebarFacet sidebarFacet = new FacetConfigurationRest.SidebarFacet();
        sidebarFacet.setType("date");
        sidebarFacet.setName("dateName");

        facetConfigurationRest.addSidebarFacet(sidebarFacet);

        assertEquals(sidebarFacet, facetConfigurationRest.getSidebarFacets().get(0));
    }

}
