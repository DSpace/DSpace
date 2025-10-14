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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.hamcrest.Matcher;

public class SortOptionMatcher {

    private SortOptionMatcher() { }

    public static Matcher<? super Object> titleSortOption() {
        return allOf(
            hasJsonPath("$.name", is("dc.title"))
        );
    }

    public static Matcher<? super Object> dateIssuedSortOption() {
        return allOf(
            hasJsonPath("$.name", is("dc.date.issued"))
        );
    }

    public static Matcher<? super Object> dateAccessionedSortOption() {
        return allOf(
                hasJsonPath("$.name", is("dc.date.accessioned"))
        );
    }

    public static Matcher<? super Object> scoreSortOption() {
        return allOf(
            hasJsonPath("$.name", is("score"))
        );
    }

    public static Matcher<? super Object> sortByAndOrder(String by, String order) {
        return allOf(
            hasJsonPath("$.by", is(by)),
            hasJsonPath("$.order", is(order))
        );
    }

    public static Matcher<? super Object> sortOptionMatcher(String name, String sortDirection) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.sortOrder", is(sortDirection))
        );
    }


    public static Matcher<? super Object> matchSortFieldConfiguration(DiscoverySortFieldConfiguration sortFieldConfiguration) {

        Matcher<? super Object> nameMatcher;
        if (sortFieldConfiguration.getMetadataField() == null) {
            nameMatcher = nullValue();
        } else {
            nameMatcher = is(sortFieldConfiguration.getMetadataField());
        }

        Matcher<? super Object> typeMatcher;
        if (sortFieldConfiguration.getType() == null) {
            typeMatcher = nullValue();
        } else {
            typeMatcher = is(sortFieldConfiguration.getType());
        }

        return allOf(
            hasJsonPath("$.name", nameMatcher),
            hasJsonPath("$.sortOrder", is(sortFieldConfiguration.getDefaultSortOrder().toString())),
            hasJsonPath("$.type", typeMatcher)
        );
    }


    public static Matcher<? super Object>[] createSortOptionMatchers(List<DiscoverySortFieldConfiguration> sortFields) {
        return sortFields.stream()
            .map(SortOptionMatcher::matchSortFieldConfiguration)
            .toArray(Matcher[]::new);
    }
}
