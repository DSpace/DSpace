package org.dspace.app.rest.link.search;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by raf on 26/09/2017.
 */
public class SearchResultsResourceHalLinkFactoryTest {
    SearchResultsResourceHalLinkFactory searchResultsResourceHalLinkFactory;
    @Before
    public void setUp() throws Exception{
        searchResultsResourceHalLinkFactory  = new SearchResultsResourceHalLinkFactory();
    }

    @Test
    public void testControllerClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(DiscoveryRestController.class, searchResultsResourceHalLinkFactory.getControllerClass());
    }

    @Test
    public void testResourceClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(SearchResultsResource.class, searchResultsResourceHalLinkFactory.getResourceClass());
    }
}
