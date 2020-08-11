/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.dspace.app.rest.model.FacetConfigurationRest;
import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the FacetConfigurationRest class
 */
public class FacetConfigurationResourceTest {
    private FacetConfigurationRest facetConfigurationRest;

    @Before
    public void setUp() throws Exception {
        facetConfigurationRest = new FacetConfigurationRest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullThrowsException() throws Exception {
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(null);
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception {
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(facetConfigurationRest);
        assertNotNull(facetConfigurationResource);
        assertNotNull(facetConfigurationResource.getContent());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception {
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(facetConfigurationRest);
        assertEquals(facetConfigurationRest, facetConfigurationResource.getContent());
    }

}
