/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

/**
 * This matcher has been created so that we can use a predefined Matcher class to verify Authority Entries
 */
public class AuthorityEntryMatcher {

    private AuthorityEntryMatcher() {
    }

    public static Matcher<? super Object> matchAuthorityEntry(String id, String display, String value) {
        return allOf(
            matchProperties(id, display, value),
            matchLinks());
    }

    public static Matcher<? super Object> matchLinks() {
        return allOf(
            hasJsonPath("$._links.self.href", containsString("api/integration/authority/")));
    }

    private static Matcher<? super Object> matchProperties(String id, String display, String value) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.display", is(display)),
                hasJsonPath("$.value", is(value)),
                hasJsonPath("$.type", is("authority"))
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "authorityEntries"
        );
    }
}
