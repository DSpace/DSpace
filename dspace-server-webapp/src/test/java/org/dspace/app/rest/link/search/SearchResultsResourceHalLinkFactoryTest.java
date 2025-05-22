/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import static org.junit.jupiter.api.Assertions.assertEquals;


import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class' purpose is to test the SearchResultsResourceHalLinkFactory
 */
public class SearchResultsResourceHalLinkFactoryTest {
    SearchResultsResourceHalLinkFactory searchResultsResourceHalLinkFactory;

    @BeforeEach
    public void setUp() throws Exception {
        searchResultsResourceHalLinkFactory = new SearchResultsResourceHalLinkFactory();
    }

    @Test
    public void testControllerClassIsSetCorrectlyAfterConstructor() throws Exception {
        assertEquals(DiscoveryRestController.class, searchResultsResourceHalLinkFactory.getControllerClass());
    }

    @Test
    public void testResourceClassIsSetCorrectlyAfterConstructor() throws Exception {
        assertEquals(SearchResultsResource.class, searchResultsResourceHalLinkFactory.getResourceClass());
    }
}
