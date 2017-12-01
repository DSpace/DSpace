package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class GroupMatcher {

    public static Matcher<? super Object> matchGroupEntry(UUID uuid, String name) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("group")),
                hasJsonPath("$._links.self.href", containsString("/api/eperson/groups/" + uuid.toString()))
        );
    }

    public static Matcher<? super Object> matchGroupWithName(String name) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("group"))
        );
    }


}
