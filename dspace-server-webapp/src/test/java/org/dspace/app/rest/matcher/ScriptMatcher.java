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

import org.apache.commons.cli.Options;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class ScriptMatcher {

    private ScriptMatcher() {
    }

    public static Matcher<? super Object> matchScript(String name, String description) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.description", is(description))
        );
    }

    public static Matcher<? super Object> matchMockScript(Options options) {
        return allOf(
            matchScript("mock-script", "Mocking a script for testing purposes"),
            hasJsonPath("$.parameters", Matchers.containsInAnyOrder(
                ParameterMatcher.matchParameter(options.getOption("r")),
                ParameterMatcher.matchParameter(options.getOption("i"))
            ))
        );
    }
}
