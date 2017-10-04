/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchSupportHalLinkFactoryTest {
    SearchSupportHalLinkFactory searchSupportHalLinkFactory;
    @Before
    public void setUp() throws Exception{
        searchSupportHalLinkFactory  = new SearchSupportHalLinkFactory();
    }

    @Test
    public void testControllerClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(DiscoveryRestController.class, searchSupportHalLinkFactory.getControllerClass());
    }

    @Test
    public void testResourceClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(SearchSupportResource.class, searchSupportHalLinkFactory.getResourceClass());
    }

}
