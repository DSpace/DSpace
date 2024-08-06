/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class SuggestionSourceMatcher {

    private SuggestionSourceMatcher() { }

    // Matcher for a suggestion target
    public static Matcher<? super Object> matchSuggestionSource(String name, int total) {
        return Matchers.allOf(
                hasJsonPath("$.id", is(name)),
                hasJsonPath("$.total", is(total)),
                hasJsonPath("$.type", is("suggestionsource"))
        );
    }
}
