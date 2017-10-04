package org.dspace.app.rest.model;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.RootRestResourceController;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchSupportRestTest {
    SearchSupportRest searchSupportRest;

    @Before
    public void setUp() throws Exception{
        searchSupportRest = new SearchSupportRest();
    }

    @Test
    public void testConstructorDoesNotReturnNull() throws Exception{
        assertNotNull(searchSupportRest);
    }

    @Test
    public void testGetTypeReturnsCorrectValue() throws Exception{
        assertEquals(SearchSupportRest.NAME, searchSupportRest.getType());
    }

    @Test
    public void testGetCategoryReturnsCorrectValue() throws Exception{
        assertEquals(SearchSupportRest.CATEGORY, searchSupportRest.getCategory());
    }

    @Test
    public void testGetControllerReturnsCorrectValue() throws Exception{
        assertEquals(DiscoveryRestController.class, searchSupportRest.getController());
    }
}
