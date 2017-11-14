/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;

import org.hamcrest.Matcher;

public class FacetEntryMatcher {

    public FacetEntryMatcher(){

    }
    public static Matcher<? super Object> authorFacet() {
        return allOf(
                hasJsonPath("$.name", is("author")),
                hasJsonPath("$.facetType", is("text")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/author"))
        );
    }

    public static Matcher<? super Object> authorFacetInSearchObject(boolean hasMore) {
        return allOf(
                hasJsonPath("$.name", is("author")),
                hasJsonPath("$.facetType", is("text")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/author")),
                hasJsonPath("$.hasMore", is(hasMore))
        );
    }


    public static Matcher<? super Object> subjectFacet() {
        return allOf(
                hasJsonPath("$.name", is("subject")),
                hasJsonPath("$.facetType", is("hierarchical")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/subject"))
        );
    }

    public static Matcher<? super Object> subjectFacetInSearchObject(boolean hasMore) {
        return allOf(
                hasJsonPath("$.name", is("subject")),
                hasJsonPath("$.facetType", is("hierarchical")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/subject")),
                hasJsonPath("$.hasMore", is(hasMore))

        );
    }

    public static Matcher<? super Object> dateIssuedFacet() {
        return allOf(
                hasJsonPath("$.name", is("dateIssued")),
                hasJsonPath("$.facetType", is("date")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued"))
        );
    }

    public static Matcher<? super Object> dateIssuedFacetInSearchObject(boolean hasMore) {
        return allOf(
                hasJsonPath("$.name", is("dateIssued")),
                hasJsonPath("$.facetType", is("date")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")),
                hasJsonPath("$.hasMore", is(hasMore))
        );
    }

    public static Matcher<? super Object> hasContentInOriginalBundleFacet() {
        return allOf(
                hasJsonPath("$.name", is("has_content_in_original_bundle")),
                hasJsonPath("$.facetType", is("standard")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/has_content_in_original_bundle"))
        );
    }

    public static Matcher<? super Object> hasContentInOriginalBundleFacetInSearchObjects(boolean hasMore) {
        return allOf(
                hasJsonPath("$.name", is("has_content_in_original_bundle")),
                hasJsonPath("$.facetType", is("standard")),
                hasJsonPath("$._links.self.href", containsString("api/discover/facets/has_content_in_original_bundle")),
                hasJsonPath("$.hasMore", is(hasMore))
        );
    }

}
