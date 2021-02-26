/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import static org.junit.Assert.assertEquals;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.junit.Before;
import org.junit.Test;

/**
 * This class' purpose is to test the SearchSupportHalLinkFactory class
 */
public class SearchSupportHalLinkFactoryTest {
    SearchSupportHalLinkFactory searchSupportHalLinkFactory;

    @Before
    public void setUp() throws Exception {
        searchSupportHalLinkFactory = new SearchSupportHalLinkFactory();
    }

    @Test
    public void testControllerClassIsSetCorrectlyAfterConstructor() throws Exception {
        assertEquals(DiscoveryRestController.class, searchSupportHalLinkFactory.getControllerClass());
    }

    @Test
    public void testResourceClassIsSetCorrectlyAfterConstructor() throws Exception {
        assertEquals(SearchSupportResource.class, searchSupportHalLinkFactory.getResourceClass());
    }

}
