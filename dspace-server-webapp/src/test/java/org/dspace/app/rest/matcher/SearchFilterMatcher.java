/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;

public class SearchFilterMatcher {

    private SearchFilterMatcher() { }

    public static final DiscoveryConfiguration defaultConfig =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getServiceByName("defaultConfiguration", DiscoveryConfiguration.class);
    public static final List<DiscoverySearchFilterFacet> sidebarFacets = defaultConfig.getSidebarFacets();
    public static final List<DiscoverySearchFilter> searchFilters = defaultConfig.getSearchFilters();
    public static final Matcher<? super Object>[] searchFilterMatchers = createSearchFilterMatchers(searchFilters);

    public static Matcher<? super Object> matchSearchFilter(DiscoverySearchFilter searchFilter) {
        return allOf(
                hasJsonPath("$.filter", is(searchFilter.getIndexFieldName())),
                hasJsonPath("$.hasFacets", is(isFacet(searchFilter))),
                hasJsonPath("$.type", is(searchFilter.getType())),
                hasJsonPath("$.openByDefault", is(searchFilter.isOpenByDefault())),
                checkOperators()
        );
    }

    public static Matcher<? super Object>[] createSearchFilterMatchers(List<DiscoverySearchFilter> searchFilters) {
        return searchFilters.stream()
                            .map(SearchFilterMatcher::matchSearchFilter)
                            .toArray(Matcher[]::new);
    }

    public static boolean isFacet(DiscoverySearchFilter sf) {
        return sidebarFacets.stream().anyMatch(f -> f.equals(sf));
    }

    public static Matcher<? super Object> hasGeospatialMetadataFilter() {
        return allOf(
                hasJsonPath("$.filter", is("has_geospatial_metadata")),
                checkOperators()
        );
    }

    public static Matcher<? super Object> checkOperators() {
        return allOf(
                hasJsonPath("$.operators",  containsInAnyOrder(
                        hasJsonPath("$.operator", is("equals")),
                        hasJsonPath("$.operator", is("notequals")),
                        hasJsonPath("$.operator", is("authority")),
                        hasJsonPath("$.operator", is("notauthority")),
                        hasJsonPath("$.operator", is("contains")),
                        hasJsonPath("$.operator", is("notcontains")),
                        hasJsonPath("$.operator", is("query"))
                        ))
        );
    }
}
