package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class FacetResultsResource extends HALResource{

    @JsonUnwrapped
    private final FacetResultsRest data;

    @JsonUnwrapped
    private EmbeddedPage embeddedPage;

    @JsonIgnore
    private String baseLinkString;

    public FacetResultsResource(FacetResultsRest facetResultsRest, Utils utils){
        this.data = facetResultsRest;
        addEmbeds(facetResultsRest, utils);
    }

    public void addEmbeds(final FacetResultsRest data, final Utils utils) {
        List<SearchFacetValueResource> list = buildEntryList(data, utils);

        if(StringUtils.isNotBlank(baseLinkString)){
            Page<SearchFacetValueResource> pageImpl = new PageImpl<>(list, data.getPage(), list.size() + (data.isHasMore() ? 1 : 0));
            embeddedPage = new EmbeddedPage(baseLinkString, pageImpl, list, false);
        }

        embedResource("values", list);
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
