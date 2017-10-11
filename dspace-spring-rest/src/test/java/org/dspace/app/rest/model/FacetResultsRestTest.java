package org.dspace.app.rest.model;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class FacetResultsRestTest {

    FacetResultsRest facetResultsRest;

    @Before
    public void setUp() throws Exception{
        facetResultsRest = new FacetResultsRest();
    }

    @Test
    public void testFacetResultListNotNullAfterConstructor() throws Exception{
        assertNotNull(facetResultsRest.getFacetResultList());
    }

    @Test
    public void testAddToFacetResultListContainsCorrectValue() throws Exception{
        SearchFacetValueRest searchFacetValueRest = new SearchFacetValueRest();
        facetResultsRest.addToFacetResultList(searchFacetValueRest);
        assertEquals(searchFacetValueRest, facetResultsRest.getFacetResultList().get(0));
    }

}
