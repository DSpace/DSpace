package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.*;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

public class FacetResultsResource extends HALResource{

    @JsonUnwrapped
    private final FacetResultsRest data;

    @JsonUnwrapped
    private EmbeddedPage embeddedPage;


    public FacetResultsResource(FacetResultsRest facetResultsRest, Pageable page, Utils utils){
        this.data = facetResultsRest;
        addEmbeds(facetResultsRest, page, utils);
    }

    private void addEmbeds(final FacetResultsRest data, final Pageable page, final Utils utils) {
        List<SearchFacetValueResource> list = buildEntryList(data);
        Page<SearchFacetValueResource> pageImpl = new PageImpl<>(list, page, -1);
        embeddedPage = new EmbeddedPage(buildBaseLink(data), pageImpl, list);
        embedResource("values", embeddedPage.getPageContent());
    }
    private static List<SearchFacetValueResource> buildEntryList(final FacetResultsRest data) {
//        LinkedList<FacetResultEntryResource> list = new LinkedList<>();
//        for(SearchFacetValueRest searchFacetValueRest : data.getFacetResultList()){
//            FacetResultEntryRest facetResultEntryRest = new FacetResultEntryRest();
//            facetResultEntryRest.setName(searchFacetValueRest.getLabel());
//            facetResultEntryRest.setCount(searchFacetValueRest.getCount());
//            FacetResultEntryResource facetResultEntryResource = new FacetResultEntryResource(facetResultEntryRest);
//            list.add(facetResultEntryResource);
//        }
//        return list;

        LinkedList<SearchFacetValueResource> list = new LinkedList<>();
        for(SearchFacetValueRest searchFacetValueRest : data.getFacetResultList()){
            SearchFacetValueResource searchFacetValueResource = new SearchFacetValueResource(searchFacetValueRest, );
            list.add(searchFacetValueResource);
        }
        return list;
    }
    public FacetResultsRest getData(){
        return data;
    }
    private String buildBaseLink(final FacetResultsRest data) {

        //TODO MOVE -> factory
        DiscoveryRestController methodOn = methodOn(DiscoveryRestController.class);

        UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
                .getFacetValues(data.getName(), data.getQuery(), data.getDsoType(), data.getScope(), null, null))
                .toUriComponentsBuilder();

        return addFilterParams(uriComponentsBuilder).build().toString();
    }

    private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder) {
        if (data.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : data.getAppliedFilters()) {
                uriComponentsBuilder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }

        return uriComponentsBuilder;
    }
}
