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

import org.dspace.content.Item;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class SuggestionMatcher {

    private SuggestionMatcher() { }

    // Matcher for a suggestion
    public static Matcher<? super Object> matchSuggestion(String source, Item target, String display,
            String suggestionId) {
        return Matchers.allOf(
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.source", is(source)),
                hasJsonPath("$.id", is(source + ":" + target.getID().toString() + ":" + suggestionId)),
                hasJsonPath("$.metadata['dc.title'][0].value", is("Title Suggestion " + suggestionId )),
                hasJsonPath("$.metadata['dc.source'][0].value", is("Source 1")),
                hasJsonPath("$.metadata['dc.source'][1].value", is("Source 2")),
                hasJsonPath("$.score"),
                hasJsonPath("$.evidences"),
                hasJsonPath("$.type", is("suggestion"))
        );
    }

    public static Matcher<? super Object> matchSuggestion(String source, Item target, String display,
            String suggestionId, double score, String evidenceName, double evidenceScore, String evidenceNote) {
        return Matchers.allOf(
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.source", is(source)),
                hasJsonPath("$.id", is(source + ":" + target.getID().toString() + ":" + suggestionId)),
                hasJsonPath("$.metadata['dc.title'][0].value", is("Title Suggestion " + suggestionId )),
                hasJsonPath("$.metadata['dc.source'][0].value", is("Source 1")),
                hasJsonPath("$.metadata['dc.source'][1].value", is("Source 2")),
                hasJsonPath("$.score", is(String.format("%.2f", score))),
                hasJsonPath("$.evidences." + evidenceName, Matchers.is(
                        hasJsonPath("$",
                                Matchers.allOf(
                                        hasJsonPath("$.score", is(String.format("%.2f", evidenceScore))),
                                        hasJsonPath("$.notes", is(evidenceNote))))
                        )),
                hasJsonPath("$.type", is("suggestion"))
        );
    }

}
