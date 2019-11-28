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

import org.apache.commons.cli.Option;
import org.hamcrest.Matcher;

public class ParameterMatcher {

    private ParameterMatcher() {
    }

    public static Matcher<? super Object> matchParameter(Option option) {
        return allOf(
            hasJsonPath("$.name", is("-" + option.getOpt())),
            hasJsonPath("$.description", is(option.getDescription())),
            hasJsonPath("$.type", is(((Class) option.getType()).getSimpleName()))
        );
    }
}
