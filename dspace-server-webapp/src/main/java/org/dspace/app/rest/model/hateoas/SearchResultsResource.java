/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to create a container with a list of the SearchResultEntryResources
 */
@RelNameDSpaceResource(SearchResultsRest.NAME)
public class SearchResultsResource extends DSpaceResource<SearchResultsRest> {

    @JsonIgnore
    private List<SearchResultEntryResource> entryResources;

    public SearchResultsResource(final SearchResultsRest data, final Utils utils) {
        super(data, utils);
        addEmbeds(data, utils);
    }

    private void addEmbeds(final SearchResultsRest data, final Utils utils) {
        embedSearchResults(data, utils);
    }

    private void embedSearchResults(final SearchResultsRest data, final Utils utils) {
        entryResources = new LinkedList<>();
        for (SearchResultEntryRest searchResultEntry : CollectionUtils.emptyIfNull(data.getSearchResults())) {
            entryResources.add(new SearchResultEntryResource(searchResultEntry, utils));
        }

        Page page = new PageImpl<>(entryResources, data.getPage(), data.getTotalNumberOfResults());

        EmbeddedPage embeddedPage = new EmbeddedPage(constructSelfLink(data),
                page, entryResources, "objects");
        embedResource("searchResult", embeddedPage);
    }

    public String constructSelfLink(SearchResultsRest data) {
        UriComponentsBuilder builder = linkTo(data.getController(), data.getCategory(), data.getTypePlural())
            .slash("search/objects").toUriComponentsBuilder();
        builder.queryParam("query", data.getQuery());
        for (SearchResultsRest.AppliedFilter filter : CollectionUtils.emptyIfNull(data.getAppliedFilters())) {
            builder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
        }
        if (StringUtils.isNotBlank(data.getScope())) {
            builder.queryParam("scope", data.getScope());
        }
        if (StringUtils.isNotBlank(data.getConfiguration())) {
            builder.queryParam("configuration", data.getConfiguration());
        }
        if (CollectionUtils.isNotEmpty(data.getDsoTypes())) {
            builder.queryParam("dsoType", data.getDsoTypes());
        }
        return builder.toUriString();
    }
}
