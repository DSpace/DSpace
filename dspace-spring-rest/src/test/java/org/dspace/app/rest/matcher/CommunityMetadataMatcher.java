package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class CommunityMetadataMatcher {

    public static Matcher<? super Object> matchTitle(String title){
        return allOf(
                hasJsonPath("$.key", is("dc.title")),
                hasJsonPath("$.value", is(title))
        );
    }
}
