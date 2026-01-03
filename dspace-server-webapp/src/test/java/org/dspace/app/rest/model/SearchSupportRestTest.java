/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.dspace.app.rest.DiscoveryRestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class' purpose is to test the SearchSupportRest class
 */
public class SearchSupportRestTest {
    SearchSupportRest searchSupportRest;

    @BeforeEach
    public void setUp() throws Exception {
        searchSupportRest = new SearchSupportRest();
    }

    @Test
    public void testConstructorDoesNotReturnNull() throws Exception {
        assertNotNull(searchSupportRest);
    }

    @Test
    public void testGetTypeReturnsCorrectValue() throws Exception {
        assertEquals(SearchSupportRest.NAME, searchSupportRest.getType());
    }

    @Test
    public void testGetCategoryReturnsCorrectValue() throws Exception {
        assertEquals(SearchSupportRest.CATEGORY, searchSupportRest.getCategory());
    }

    @Test
    public void testGetControllerReturnsCorrectValue() throws Exception {
        assertEquals(DiscoveryRestController.class, searchSupportRest.getController());
    }
}
