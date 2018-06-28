/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.hateoas.FacetConfigurationResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;


/**
 * This class' purpose is to add the links to the FacetConfigurationResource. This function and class will be called
 * and used
 * when the HalLinkService addLinks methods is called as it'll iterate over all the different factories and check
 * whether
 * these are allowed to create links for said resource or not.
 */
@Component
public class FacetConfigurationResourceHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetConfigurationResource> {

    protected void addLinks(FacetConfigurationResource halResource, Pageable page, LinkedList<Link> list)
        throws Exception {
        FacetConfigurationRest data = halResource.getContent();

        if (data != null) {
            list.add(buildLink(Link.REL_SELF, getMethodOn()
                .getFacetsConfiguration(data.getScope(), data.getConfiguration(), page)));
        }
    }

    protected Class<FacetConfigurationResource> getResourceClass() {
        return FacetConfigurationResource.class;
    }

}
