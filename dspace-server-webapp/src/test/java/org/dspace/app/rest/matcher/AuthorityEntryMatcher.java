package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class AuthorityEntryMatcher {

    private AuthorityEntryMatcher() {
    }

    public static Matcher<? super Object> matchAuthorityEntry(String id, String display, String value) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.display", is(display)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.type", is("authority")),
            matchLinks());
    }

    private static Matcher<? super Object> matchLinks() {
        return allOf(
            hasJsonPath("$._links.self.href", containsString("api/integration/authority/")));
    }
}
