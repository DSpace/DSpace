/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class FacetConfigurationRestTest {

    FacetConfigurationRest facetConfigurationRest;

    @Before
    public void setUp() throws Exception {
        facetConfigurationRest = new FacetConfigurationRest();
    }

    @Test
    public void testSidebarFacetsNotNullAfterConstructor() {
        assertNotNull(facetConfigurationRest.getSidebarFacets());
    }

    @Test
    public void testAddSidebarFacetsContainsCorrectSidebarFacet() {
        SearchFacetEntryRest sidebarFacet = new SearchFacetEntryRest("dateName");
        sidebarFacet.setFacetType("date");

        facetConfigurationRest.addSidebarFacet(sidebarFacet);

        assertEquals(sidebarFacet, facetConfigurationRest.getSidebarFacets().get(0));
    }

}
