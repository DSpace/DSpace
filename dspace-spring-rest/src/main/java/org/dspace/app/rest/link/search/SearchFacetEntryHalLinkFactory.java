/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.SearchFacetEntryResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This factory provides a means to add links to the SearchFacetEntryResource. This class and addLinks method will be
 * called
 * from the HalLinkService addLinks method is called if the HalResource given is eligible
 */
@Component
public class SearchFacetEntryHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchFacetEntryResource> {

    @Override
    protected void addLinks(SearchFacetEntryResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        SearchFacetEntryRest facetData = halResource.getFacetData();
        DiscoveryResultsRest searchData = halResource.getSearchData();

        String query = searchData == null ? null : searchData.getQuery();
        String dsoType = searchData == null ? null : searchData.getDsoType();
        String scope = searchData == null ? null : searchData.getScope();
        String configuration = searchData == null ? null : searchData.getConfiguration();

        UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn().getFacetValues(facetData.getName(), null, query,
                dsoType, scope, configuration, null, null));

        addFilterParams(uriBuilder, searchData);

        //If our rest data contains a list of values, construct the page links. Otherwise, only add a self link
        if (CollectionUtils.isNotEmpty(facetData.getValues())) {
            PageImpl page = new PageImpl<>(facetData.getValues(), new PageRequest(0, facetData.getFacetLimit()),
                                           facetData.getValues().size() + (BooleanUtils
                                               .isTrue(facetData.isHasMore()) ? 1 : 0));

            halResource.setPageHeader(new EmbeddedPageHeader(uriBuilder, page, false));

        } else {
            list.add(buildLink(Link.REL_SELF, uriBuilder.build().toUriString()));
        }

    }

    @Override
    protected Class<SearchFacetEntryResource> getResourceClass() {
        return SearchFacetEntryResource.class;
    }

}
