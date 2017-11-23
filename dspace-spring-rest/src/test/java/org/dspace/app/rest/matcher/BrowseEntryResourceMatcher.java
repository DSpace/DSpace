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
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.*;

public class BrowseEntryResourceMatcher {
    public static Matcher<? super Object> matchBrowseEntry(String value, int expectedCount) {
        return allOf(
                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.count", is(expectedCount)),
                //Check links
                matchItemLinks()
        );
    }
    public static Matcher<? super Object> matchItemLinks() {
        return allOf(
                hasJsonPath("$._links.items.href", startsWith(REST_SERVER_URL))
        );
    }
}
