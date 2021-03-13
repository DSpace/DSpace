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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class SearchResultMatcher {

    private SearchResultMatcher() { }

    public static Matcher<? super Object> match(String category, String type, String typePlural) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/" + category + "/" + typePlural)),
            hasJsonPath("$._embedded", notNullValue()),
            hasJsonPath("$._embedded.indexableObject", is(
                matchEmbeddedObject(type)
            ))
        );
    }

    public static Matcher<? super Object> match() {
        return allOf(
            hasJsonPath("$.type", is("discover"))
        );
    }

    private static Matcher<? super Object> matchEmbeddedObject(String type) {
        return allOf(
            Matchers.anyOf(
                allOf(
                    hasJsonPath("$.uuid", notNullValue()),
                    hasJsonPath("$.name", notNullValue())
                ),
                hasJsonPath("$.id", notNullValue())
            ),
            hasJsonPath("$.type", is(type))
        );
    }

    public static Matcher<? super Object> matchOnItemName(String type, String typePlural, String itemName) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/core/" + typePlural)),
            hasJsonPath("$._embedded", notNullValue()),
            hasJsonPath("$._embedded.indexableObject", is(
                matchEmbeddedObjectOnItemName(type, itemName)
            ))
        );
    }

    private static Matcher<? super Object> matchEmbeddedObjectOnItemName(String type, String itemName) {
        return allOf(
            hasJsonPath("$.uuid", notNullValue()),
            hasJsonPath("$.name", is(itemName)),
            hasJsonPath("$.type", is(type))
        );
    }

    public static Matcher<? super Object> matchOnItemNameAndHitHighlight(String type, String typePlural,
                                                                         String itemName, String hitHighlightQuery,
                                                                         String expectedFieldInHitHighlightning) {
        return allOf(
            hasJsonPath("$.type", is("discover")),
            hasJsonPath("$.hitHighlights", is(
                HitHighlightMatcher.entry(hitHighlightQuery, expectedFieldInHitHighlightning))),
            hasJsonPath("$._links.indexableObject.href", containsString("/api/core/" + typePlural)),
            hasJsonPath("$._embedded", notNullValue()),
            hasJsonPath("$._embedded.indexableObject", is(
                matchEmbeddedObjectOnItemName(type, itemName)
            ))
        );
    }
}
