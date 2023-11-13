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
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.link.search.SearchResultsResourceHalLinkFactory;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * This class' purpose is to create a container with a list of the SearchResultEntryResources
 */
@RelNameDSpaceResource(SearchResultsRest.NAME)
public class SearchResultsResource extends HALResource<SearchResultsRest> {

    @JsonIgnore
    private List<SearchResultEntryResource> entryResources;

    public SearchResultsResource(final SearchResultsRest data, final Utils utils, Pageable pageable) {
        super(data);
        addEmbeds(data, utils, pageable);

    }
    private void addEmbeds(final SearchResultsRest data, final Utils utils, Pageable pageable) {
        embedSearchResults(data, utils, pageable);

        embedFacetResults(data);
    }
    private void embedFacetResults(final SearchResultsRest data) {
        List<SearchFacetEntryResource> facetResources = new LinkedList<>();
        for (SearchFacetEntryRest searchFacetEntryRest : CollectionUtils.emptyIfNull(data.getFacets())) {
            facetResources.add(new SearchFacetEntryResource(searchFacetEntryRest, data));
        }

        embedResource("facets", facetResources);
    }
    private void embedSearchResults(final SearchResultsRest data, final Utils utils, Pageable pageable) {
        entryResources = new LinkedList<>();
        for (SearchResultEntryRest searchResultEntry : CollectionUtils.emptyIfNull(data.getSearchResults())) {
            entryResources.add(new SearchResultEntryResource(searchResultEntry, utils));
        }

        // FIX for mismatched page.getTotalElements() and data.getTotalNumberOfResults()
        Page<SearchResultEntryResource> page =
                new SearchResultPageImpl<>(entryResources, pageable, data.getTotalNumberOfResults());

        SearchResultsResourceHalLinkFactory linkFactory = new SearchResultsResourceHalLinkFactory();
        EmbeddedPage embeddedPage = new EmbeddedPage(linkFactory.buildSearchBaseLink(data).toUriString(),
                page, entryResources, "objects");
        embedResource("searchResult", embeddedPage);
    }

    /**
     * This class is a reiteration of org.springframework.data.domain.PageImpl, for the specific purpose of providing
     * correct 'totalElements' value for SearchResultsResource#embedSearchResults.
     * Secondly, we override every method that use PageImpl.total with SearchResultPageImpl.totalElements,
     * to make sure the value provided via RestAPI is correct.
     * @author Bui Thai Hai (thaihai.bui@dlcorp.com.vn)
     */
    private static final class SearchResultPageImpl<T> extends PageImpl<T> {
        private final long totalElements;
        public SearchResultPageImpl(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);

            // Main difference from PageImpl, where we mitigate the "mitigation" from super() which sets the 'total'
            // value with content.size()
            this.totalElements = super.getTotalElements() != total ? total : super.getTotalElements();
        }

        @Override
        public int getTotalPages() {
            return getSize() == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) getSize());
        }

        @Override
        public long getTotalElements() {
            return totalElements;
        }

        @Override
        public <U> Page<U> map(Function<? super T, ? extends U> converter) {
            return new SearchResultPageImpl<>(getConvertedContent(converter), getPageable(), totalElements);
        }


        @Override
        public boolean equals(@Nullable Object obj) {

            if (this == obj) {
                return true;
            }

            if (!(obj instanceof SearchResultPageImpl<?>)) {
                return false;
            }

            SearchResultPageImpl<?> that = (SearchResultPageImpl<?>) obj;

            return this.totalElements == that.totalElements && super.equals(obj);
        }

        @Override
        public int hashCode() {

            int result = 17;

            result += 31 * (int) (totalElements ^ totalElements >>> 32);
            result += 31 * super.hashCode();

            return result;
        }
    }
}
