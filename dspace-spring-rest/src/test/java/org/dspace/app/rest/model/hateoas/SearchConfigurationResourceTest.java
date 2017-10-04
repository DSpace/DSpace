/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchConfigurationRest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchConfigurationResourceTest {
    private SearchConfigurationRest searchConfigurationRest;

    @Before
    public void setUp() throws Exception{
        searchConfigurationRest = new SearchConfigurationRest();
    }

    @Test
    public void testConstructorWithNullStillMakesObject() throws Exception{
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(null);
        assertNotNull(searchConfigurationResource);
    }

    @Test
    public void testConstructorWithNullDataIsNull() throws Exception{
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(null);
        assertNull(searchConfigurationResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception{
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(searchConfigurationRest);
        assertNotNull(searchConfigurationResource);
        assertNotNull(searchConfigurationResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception{
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(searchConfigurationRest);
        assertEquals(searchConfigurationRest, searchConfigurationResource.getData());
    }
}
