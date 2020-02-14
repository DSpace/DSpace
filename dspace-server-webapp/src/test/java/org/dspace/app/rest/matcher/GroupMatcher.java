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
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.hamcrest.Matcher;

public class GroupMatcher {

    private GroupMatcher() { }

    public static Matcher<? super Object> matchGroupEntry(UUID uuid, String name) {
        return allOf(
                matchProperties(uuid, name)
        );
    }

    public static Matcher<? super Object> matchGroupWithName(String name) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.type", is("group")),
            hasJsonPath("$._links.self.href", containsString("/api/eperson/groups/")),
            hasJsonPath("$._links.groups.href", endsWith("/groups"))
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "groups[]"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "eperson/groups/" + uuid,
                "groups",
                "self"
        );
    }

    private static Matcher<? super Object> matchProperties(UUID uuid, String name) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("group")),
                hasJsonPath("$._links.self.href", containsString("/api/eperson/groups/" + uuid.toString())),
                hasJsonPath("$._links.groups.href", endsWith(uuid.toString() + "/groups"))
        );
    }
}
