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
 * Created by raf on 25/09/2017.
 */
@Component
public class FacetConfigurationResourceHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetConfigurationResource> {

    protected void addLinks(FacetConfigurationResource halResource, Pageable page, LinkedList<Link> list) {
        FacetConfigurationRest data = halResource.getContent();

        if(data != null){
            list.add(buildLink(Link.REL_SELF, getMethodOn()
                    .getFacetsConfiguration(data.getScope(), data.getConfigurationName())));
        }
    }

    protected Class<FacetConfigurationResource> getResourceClass() {
        return FacetConfigurationResource.class;
    }

}
