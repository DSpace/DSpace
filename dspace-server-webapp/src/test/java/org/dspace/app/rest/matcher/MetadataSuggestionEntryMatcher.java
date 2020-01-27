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
import org.hamcrest.Matchers;

public class MetadataSuggestionEntryMatcher {

    private MetadataSuggestionEntryMatcher() { }

    public static Matcher<? super Object> matchEntry(String metadataSuggestion, String id) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.display", is(id)),
            hasJsonPath("$.value", is(id)),
            hasJsonPath("$.metadataSuggestion", is(metadataSuggestion)),
            hasJsonPath("$.type", is("metadataSuggestionEntry")),
            hasJsonPath("metadata['dc.contributor.author'][0].value", Matchers.is("Donald, Smith")),
            hasJsonPath("$._links.self.href",
                        Matchers.containsString("api/integration/metadatasuggestions/"
                                                    + metadataSuggestion + "/entryValues")));
    }


}
