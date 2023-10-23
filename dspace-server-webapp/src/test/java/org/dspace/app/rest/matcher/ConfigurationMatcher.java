/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class ConfigurationMatcher {

    private ConfigurationMatcher() { }

    // Matcher for configuration
    public static Matcher<? super Object> matchConfiguration(String name) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.values", Matchers.not(Matchers.empty())),
                hasJsonPath("$.type", is("property"))
        );
    }
}
