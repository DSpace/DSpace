/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.facet;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.link.facet.FacetConfigurationResourceHalLinkFactory;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.hateoas.FacetConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created by raf on 26/09/2017.
 */
public class FacetConfigurationResourceHalLinkFactoryTest{



    @Mock
    ControllerLinkBuilder controllerLinkBuilder;

    @Mock
    HalLinkFactory halLinkFactory;

    @InjectMocks
    private FacetConfigurationResourceHalLinkFactory facetConfigurationResourceHalLinkFactory;

    @Before
    public void setUp() throws Exception{
        facetConfigurationResourceHalLinkFactory  = new FacetConfigurationResourceHalLinkFactory();
    }

    @Test
    public void testControllerClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(DiscoveryRestController.class, facetConfigurationResourceHalLinkFactory.getControllerClass());
    }

    @Test
    public void testResourceClassIsSetCorrectlyAfterConstructor() throws Exception{
        assertEquals(FacetConfigurationResource.class, facetConfigurationResourceHalLinkFactory.getResourceClass());
    }

}
