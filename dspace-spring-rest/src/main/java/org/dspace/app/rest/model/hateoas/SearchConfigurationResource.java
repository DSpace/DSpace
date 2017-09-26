package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 22/09/2017.
 */
@RelNameDSpaceResource(SearchConfigurationRest.NAME)
public class SearchConfigurationResource extends HALResource{

    @JsonUnwrapped
    private final SearchConfigurationRest data;


    public SearchConfigurationResource(SearchConfigurationRest searchConfigurationRest){
        this.data = searchConfigurationRest;


//        addLinks(data);

    }
//    private void addLinks(final SearchConfigurationRest data) {
//        //Create the self link using our Controller
//        String baseLink = buildBaseLink(data);
//
//        Link link = new Link(baseLink, Link.REL_SELF);
//        add(link);
//    }
//
//    private String buildBaseLink(final SearchConfigurationRest data) {
//        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);
//        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
//                .getSearchConfiguration(data.getScope(), data.getConfigurationName()))
//                .toUriComponentsBuilder();
//
//        return uriComponentsBuilder.build().toString();
//    }

    public SearchConfigurationRest getData(){
        return data;
    }
}
