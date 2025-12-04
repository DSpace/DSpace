/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.link.search.SearchResultsResourceHalLinkFactory;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * This class' purpose is to create a container with a list of the SearchResultEntryResources
 */
@RelNameDSpaceResource(SearchResultsRest.NAME)
public class SearchResultsResource extends HALResource<SearchResultsRest> {

    @JsonIgnore
    private List<SearchResultEntryResource> entryResources;

    public SearchResultsResource(final SearchResultsRest data, final Utils utils) {
        super(data);
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

        SearchResultsResourceHalLinkFactory linkFactory = new SearchResultsResourceHalLinkFactory();
        EmbeddedPage embeddedPage = new EmbeddedPage(linkFactory.buildSearchBaseLink(data).toUriString(),
                page, entryResources, "objects");
        embedResource("searchResult", embeddedPage);
    }
}
