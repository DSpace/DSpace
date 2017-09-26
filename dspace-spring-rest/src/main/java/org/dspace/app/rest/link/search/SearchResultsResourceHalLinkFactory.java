package org.dspace.app.rest.link.search;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchResultsResourceHalLinkFactory extends HalLinkFactory<SearchResultsResource, DiscoveryRestController> {

    protected void addLinks(SearchResultsResource halResource, LinkedList<Link> list) {
        SearchResultsRest data = halResource.getData();

        if(data != null){

            list.add(buildLink(Link.REL_SELF, getMethodOn()
                            .getSearchObjects(data.getScope(), data.getConfigurationName(), data.getScope(), data.getConfigurationName(), null, null)));

        }
    }

    protected Class<SearchResultsResource> getResourceClass() {
        return SearchResultsResource.class;
    }


    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}