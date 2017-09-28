/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchSupportRest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchSupportResourceTest {
    private SearchSupportRest searchSupportRest;

    @Before
    public void setUp() throws Exception{
        searchSupportRest = new SearchSupportRest();
    }

    @Test
    public void testConstructorWithNullStillMakesObject() throws Exception{
        SearchSupportResource searchSupportResource = new SearchSupportResource(null);
        assertNotNull(searchSupportResource);
    }

    @Test
    public void testConstructorWithNullDataIsNull() throws Exception{
        SearchSupportResource searchSupportResource = new SearchSupportResource(null);
        assertNull(searchSupportResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndObjectNotNull() throws Exception{
        SearchSupportResource searchSupportResource = new SearchSupportResource(searchSupportRest);
        assertNotNull(searchSupportResource);
        assertNotNull(searchSupportResource.getData());
    }

    @Test
    public void testConstructorAndGetterWithProperDataAndProperDataReturned() throws Exception{
        SearchSupportResource searchSupportResource = new SearchSupportResource(searchSupportRest);
        assertEquals(searchSupportRest, searchSupportResource.getData());
    }
}
