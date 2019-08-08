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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.hamcrest.Matcher;

public class GroupMatcher {

    private GroupMatcher() { }

    public static Matcher<? super Object> matchGroupEntry(UUID uuid, String name) {
        return allOf(
            hasJsonPath("$.uuid", is(uuid.toString())),
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.type", is("group")),
            hasJsonPath("$._links.self.href", containsString("/api/eperson/groups/" + uuid.toString())),
            hasJsonPath("$._links.groups.href", endsWith(uuid.toString() + "/groups"))
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

}
