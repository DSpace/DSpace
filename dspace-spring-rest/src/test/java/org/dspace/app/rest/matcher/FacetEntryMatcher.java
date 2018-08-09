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
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.hamcrest.Matcher;
import org.hamcrest.core.AnyOf;

public class FacetEntryMatcher {

    private FacetEntryMatcher() {
    }

    public static Matcher<? super Object> authorFacet(boolean hasNext) {
        return allOf(
            hasJsonPath("$.name", is("author")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/author")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/author"))
        );
    }

    public static Matcher<? super Object> authorFacetWithMinMax(boolean hasNext, String min, String max) {
        return allOf(
            hasJsonPath("$.name", is("author")),
            hasJsonPath("$.facetType", is("text")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.minValue", is(min)),
            hasJsonPath("$.maxValue", is(max)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/author")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/author"))
        );
    }

    public static Matcher<? super Object> subjectFacet(boolean hasNext) {
        return allOf(
            hasJsonPath("$.name", is("subject")),
            hasJsonPath("$.facetType", is("hierarchical")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/subject")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/subject"))

        );
    }

    public static Matcher<? super Object> dateIssuedFacet(boolean hasNext) {
        return allOf(
            hasJsonPath("$.name", is("dateIssued")),
            hasJsonPath("$.facetType", is("date")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/dateIssued"))
        );
    }

    public static Matcher<? super Object> dateIssuedFacetWithMinMax(boolean hasNext, String min, String max) {
        return allOf(
            hasJsonPath("$.name", is("dateIssued")),
            hasJsonPath("$.facetType", is("date")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$.minValue", is(min)),
            hasJsonPath("$.maxValue", is(max)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/dateIssued"))
        );
    }

    public static Matcher<? super Object> hasContentInOriginalBundleFacet(boolean hasNext) {
        return allOf(
            hasJsonPath("$.name", is("has_content_in_original_bundle")),
            hasJsonPath("$.facetType", is("standard")),
            hasJsonPath("$.facetLimit", any(Integer.class)),
            hasJsonPath("$._links.self.href", containsString("api/discover/facets/has_content_in_original_bundle")),
            hasJsonPath("$._links", matchNextLink(hasNext, "api/discover/facets/has_content_in_original_bundle"))
        );
    }

    private static AnyOf<? super Object> matchNextLink(boolean hasNext, String path) {

        return anyOf(hasJsonPath("$.next.href", containsString(path)),
                           not(hasJsonPath("$.next.href", containsString(path))));
    }

}
