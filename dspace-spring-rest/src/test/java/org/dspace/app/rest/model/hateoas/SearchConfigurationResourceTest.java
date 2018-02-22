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

import org.dspace.app.rest.model.SearchConfigurationRest;
import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the SearchConfigurationResource
 */
public class SearchConfigurationResourceTest {
    private SearchConfigurationRest searchConfigurationRest;

    @Before
    public void setUp() throws Exception {
        searchConfigurationRest = new SearchConfigurationRest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullThrowsException() throws Exception {
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(null);
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception {
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(
            searchConfigurationRest);
        assertNotNull(searchConfigurationResource);
        assertNotNull(searchConfigurationResource.getContent());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception {
        SearchConfigurationResource searchConfigurationResource = new SearchConfigurationResource(
            searchConfigurationRest);
        assertEquals(searchConfigurationRest, searchConfigurationResource.getContent());
    }
}
