package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class EPersonMetadataMatcher {

    public static Matcher<? super Object> matchFirstName(String firstName){
        return allOf(
                hasJsonPath("$.key", is("eperson.firstname")),
                hasJsonPath("$.value", is(firstName))
        );
    }

    public static Matcher<? super Object> matchLastName(String lastName){
        return allOf(
                hasJsonPath("$.key", is("eperson.lastname")),
                hasJsonPath("$.value", is(lastName))
        );
    }

}
