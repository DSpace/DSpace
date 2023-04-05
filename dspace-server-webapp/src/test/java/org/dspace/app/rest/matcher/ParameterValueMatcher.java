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
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class ParameterValueMatcher {

    private ParameterValueMatcher() {}

    public static Matcher<? super Object> matchParameterValue(String name, String value) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.value", is(value))
        );

    }
    public static Matcher<? super Object> matchParameterValue(String name) {
        return allOf(
            hasJsonPath("$.name", is(name))
        );

    }
}
