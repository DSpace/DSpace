package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.hateoas.SearchFacetValueResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by tom on 26/10/2017.
 */
@Component
public class SearchFacetValueHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchFacetValueResource> {
    @Override
    protected void addLinks(SearchFacetValueResource halResource, Pageable pageable, LinkedList<Link> list) {

        if(halResource.getSearchData() != null && halResource.getFacetData() != null && halResource.getValueData() != null){

            UriComponentsBuilder builder = buildSearchBaseLink(halResource.getSearchData());

            addFilterForFacetValue(builder, halResource.getSearchData(), halResource.getFacetData(), halResource.getValueData());

            list.add(buildLink("search", builder.build().toUriString()));

        }
    }

    @Override
    protected Class<SearchFacetValueResource> getResourceClass() {
        return SearchFacetValueResource.class;
    }

    private void addFilterForFacetValue(final UriComponentsBuilder baseLink, DiscoveryResultsRest searchData,
                                        SearchFacetEntryRest facetData, SearchFacetValueRest valueData) {
        //TODO ugly
        if(facetData!=null){
            baseLink.queryParam("f." + facetData.getName(), valueData.getFilterValue() + "," + valueData.getFilterType());
        }
        else{
            baseLink.queryParam("f." + ((FacetResultsRest) searchData).getName(), valueData.getLabel() + "," + "equals");
        }
    }
}
