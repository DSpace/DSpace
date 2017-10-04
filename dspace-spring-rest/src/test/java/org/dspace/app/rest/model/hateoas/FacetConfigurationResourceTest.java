/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.FacetConfigurationRest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FacetConfigurationResourceTest {
    private FacetConfigurationRest facetConfigurationRest;

    @Before
    public void setUp() throws Exception{
        facetConfigurationRest = new FacetConfigurationRest();
    }

    @Test
    public void testConstructorWithNullStillMakesObject() throws Exception{
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(null);
        assertNotNull(facetConfigurationResource);
    }

    @Test
    public void testConstructorWithNullDataIsNull() throws Exception{
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(null);
        assertNull(facetConfigurationResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception{
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(facetConfigurationRest);
        assertNotNull(facetConfigurationResource);
        assertNotNull(facetConfigurationResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception{
        FacetConfigurationResource facetConfigurationResource = new FacetConfigurationResource(facetConfigurationRest);
        assertEquals(facetConfigurationRest, facetConfigurationResource.getData());
    }

}
