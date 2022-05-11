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

import org.hamcrest.Matcher;

/**
 * 
 * 
 * @author mykhaylo
 *
 */
public class VocabularyMatcher {

    private VocabularyMatcher() {}

    public static Matcher<? super Object> matchProperties(String id, String name,
                                                           boolean scrollable, boolean hierarchical) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.scrollable", is(scrollable)),
                hasJsonPath("$.hierarchical", is(hierarchical)),
                hasJsonPath("$.type", is("vocabulary"))
        );
    }

    public static Matcher<? super Object> matchVocabularyEntry(String display, String value, String type) {
        return allOf(
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.type", is(type))
        );
    }

    public static Matcher<? super Object> matchVocabularyEntry(String display, String value, String type,
        String authority) {
        return allOf(
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.type", is(type)),
                hasJsonPath("$.authority", is(authority))
        );
    }
}
