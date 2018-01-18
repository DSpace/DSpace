/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;

public class SearchResultMatcher {


    public static Matcher<? super Object> match(String type, String typePlural) {
        return allOf(
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$._links.dspaceObject.href", containsString("/api/core/"+typePlural)),
                hasJsonPath("$._embedded", notNullValue()),
                hasJsonPath("$._embedded.dspaceObject", is(
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
                hasJsonPath("$.uuid", notNullValue()),
                hasJsonPath("$.name", notNullValue()),
                hasJsonPath("$.type", is(type))
        );
    }

    public static Matcher<? super Object> matchOnItemName(String type, String typePlural, String itemName) {
        return allOf(
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$._links.dspaceObject.href", containsString("/api/core/"+typePlural)),
                hasJsonPath("$._embedded", notNullValue()),
                hasJsonPath("$._embedded.dspaceObject", is(
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

    public static Matcher<? super Object> matchOnItemNameAndHitHighlight(String type, String typePlural, String itemName, String hitHighlightQuery, String expectedFieldInHitHighlightning) {
        return allOf(
                hasJsonPath("$.type", is("discover")),
                hasJsonPath("$.hitHighlights", is(
                        HitHighlightMatcher.entry(hitHighlightQuery, expectedFieldInHitHighlightning))),
                hasJsonPath("$._links.dspaceObject.href", containsString("/api/core/"+typePlural)),
                hasJsonPath("$._embedded", notNullValue()),
                hasJsonPath("$._embedded.dspaceObject", is(
                        matchEmbeddedObjectOnItemName(type, itemName)
                ))
        );
    }
}
