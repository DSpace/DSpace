package org.dspace.app.rest.model.hateoas;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.facet.FacetResultsResourceHalLinkFactory;
import org.dspace.app.rest.model.*;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.springframework.beans.factory.annotation.Autowired;
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

    @JsonIgnore
    private String baseLinkString;
    public FacetResultsResource(FacetResultsRest facetResultsRest, Pageable page, Utils utils){
        this.data = facetResultsRest;
        addEmbeds(facetResultsRest, page, utils);
    }

    public void addEmbeds(final FacetResultsRest data, final Pageable page, final Utils utils) {
        List<SearchFacetValueResource> list = buildEntryList(data, utils);
        Page<SearchFacetValueResource> pageImpl = new PageImpl<>(list, page, -1);
        if(!StringUtils.isNullOrEmpty(baseLinkString)){
            embeddedPage = new EmbeddedPage(baseLinkString, pageImpl, list);
            embedResource("values", embeddedPage.getPageContent());
        }
    }
    private static List<SearchFacetValueResource> buildEntryList(final FacetResultsRest data, Utils utils) {
        LinkedList<SearchFacetValueResource> list = new LinkedList<>();
        for(SearchFacetValueRest searchFacetValueRest : data.getFacetResultList()){
            SearchFacetValueResource searchFacetValueResource = new SearchFacetValueResource(searchFacetValueRest, null, data, utils);
            list.add(searchFacetValueResource);
        }
        return list;
    }
    public FacetResultsRest getData(){
        return data;
    }
    public void setBaseLinkString(String baseLinkString){
        this.baseLinkString = baseLinkString;
    }
}
