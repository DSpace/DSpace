package org.dspace.app.rest.link.search;

import org.apache.commons.lang3.ObjectUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchConfigurationResourceHalLinkFactory extends HalLinkFactory<SearchConfigurationResource, DiscoveryRestController> {

    protected void addLinks(SearchConfigurationResource halResource, LinkedList<Link> list) {
        SearchConfigurationRest data = halResource.getData();

        if(data != null){

            list.add(buildLink(Link.REL_SELF, getMethodOn()
                    .getSearchConfiguration(data.getScope(), data.getConfigurationName())));
            list.add(buildLink("objects", getMethodOn().getSearchObjects(null, null, null, null, null, null)));
        }
    }

    protected Class<SearchConfigurationResource> getResourceClass() {
        return SearchConfigurationResource.class;
    }


    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
