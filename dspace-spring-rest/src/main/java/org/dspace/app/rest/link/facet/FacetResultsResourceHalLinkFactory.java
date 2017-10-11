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
import org.dspace.app.rest.model.FacetConfigurationRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.FacetConfigurationResource;
import org.dspace.app.rest.model.hateoas.FacetResultsResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


/**
 * Created by raf on 25/09/2017.
 */
@Component
public class FacetResultsResourceHalLinkFactory extends HalLinkFactory<FacetResultsResource, DiscoveryRestController> {

    protected void addLinks(FacetResultsResource halResource, LinkedList<Link> list) {
        FacetResultsRest data = halResource.getData();

        if(data != null){

            list.add(buildLink(Link.REL_SELF, getMethodOn()
                    .getFacetValues(data.getName(), data.getQuery(), data.getDsoType(), data.getScope(), data.getSearchFilters(), data.getPage())));
        }
    }

    protected Class<FacetResultsResource> getResourceClass() {
        return FacetResultsResource.class;
    }



    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

    public String getSelfLink(FacetResultsResource halResource){

        return buildBaseLink(halResource.getData());
    }

    private String buildBaseLink(final FacetResultsRest data) {
        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);

        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
                .getFacetValues(data.getName(), data.getQuery(), data.getDsoType(), data.getScope(), null, null))
                .toUriComponentsBuilder();

        return addFilterParams(uriComponentsBuilder, data).build().toString();
    }

    private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder, FacetResultsRest data) {
        if (data.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : data.getAppliedFilters()) {
                uriComponentsBuilder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }

        return uriComponentsBuilder;
    }
}
