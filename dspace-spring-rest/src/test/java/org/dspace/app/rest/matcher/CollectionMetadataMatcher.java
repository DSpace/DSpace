package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;

public class CollectionMetadataMatcher {

    public static Matcher<? super Object> matchTitle(String title){
        return allOf(
                hasJsonPath("$.key", is("dc.title")),
                hasJsonPath("$.value", is(title))
        );
    }
}
