/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class FacetValueMatcher {

    public static Matcher<? super Object> entryAuthor(String label) {
        return allOf(
                hasJsonPath("$.label", is(label)),
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
                hasJsonPath("$._links.search.href", containsString("f.author="+label+",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssued() {
        return allOf(
                hasJsonPath("$.label", Matchers.notNullValue()),
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
                hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
                hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssuedWithCountOne() {
        return allOf(
                hasJsonPath("$.label", Matchers.notNullValue()),
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$.count", is(1)),
                hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
                hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
                hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }

    public static Matcher<? super Object> entryDateIssuedWithLabel(String label) {
        return allOf(
                hasJsonPath("$.label", is(label)),
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$._links.search.href", containsString("api/discover/search/objects")),
                hasJsonPath("$._links.search.href", containsString("f.dateIssued=")),
                hasJsonPath("$._links.search.href", containsString(",equals"))
        );
    }
}
