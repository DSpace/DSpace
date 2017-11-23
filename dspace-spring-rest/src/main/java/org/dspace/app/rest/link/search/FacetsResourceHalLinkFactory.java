/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.FacetsResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
public class FacetsResourceHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetsResource> {

    protected void addLinks(FacetsResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        SearchResultsRest data = halResource.getContent();


        if(data != null){
            list.add(buildLink(Link.REL_SELF, buildSearchFacetsBaseLink(data).build().toUriString()));
        }
    }

    protected Class<FacetsResource> getResourceClass() {
        return FacetsResource.class;
    }
}
