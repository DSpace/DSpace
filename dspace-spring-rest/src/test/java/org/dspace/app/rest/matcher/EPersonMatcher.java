package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class EPersonMatcher {

    public static Matcher<? super Object> matchEPersonEntry(UUID uuid, String name) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.type", is("eperson")),
                hasJsonPath("$._links.self.href", containsString("/api/eperson/epersons/" + uuid.toString()))
        );
    }


    public static Matcher<? super Object> matchDefaultTestEPerson() {
        return allOf(
                hasJsonPath("$.type", is("eperson"))
        );
    }
}
