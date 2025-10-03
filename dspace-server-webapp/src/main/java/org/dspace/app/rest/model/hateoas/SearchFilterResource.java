package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.SearchFilterRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

@RelNameDSpaceResource(SearchFilterRest.NAME)
public class SearchFilterResource extends HALResource<SearchFilterRest> {

    @JsonUnwrapped
    private SearchFilterRest data;

    public SearchFilterResource(SearchFilterRest content) {
        super(content);
    }

    public SearchFilterRest getData() {
        return data;
    }
}
