/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the FacetResultsRest class
 */
public class FacetResultsRestTest {

    FacetResultsRest facetResultsRest;

    @Before
    public void setUp() throws Exception {
        facetResultsRest = new FacetResultsRest();
    }

    @Test
    public void testFacetResultListNotNullAfterEntrySet() throws Exception {
        facetResultsRest.setFacetEntry(new SearchFacetEntryRest("test"));
        assertNotNull(facetResultsRest.getFacetResultList());
    }

    @Test
    public void testAddToFacetResultListContainsCorrectValue() throws Exception {
        SearchFacetValueRest searchFacetValueRest = new SearchFacetValueRest();
        facetResultsRest.setFacetEntry(new SearchFacetEntryRest("test"));
        facetResultsRest.addToFacetResultList(searchFacetValueRest);
        assertEquals(searchFacetValueRest, facetResultsRest.getFacetResultList().get(0));
    }

}
