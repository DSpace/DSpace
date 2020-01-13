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

public class ExternalSourceMatcher {

    private ExternalSourceMatcher() {
    }

    public static Matcher<? super Object> matchExternalSource(String id, String name, boolean hierarchical) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.hierarchical", is(hierarchical))
        );
    }
}
