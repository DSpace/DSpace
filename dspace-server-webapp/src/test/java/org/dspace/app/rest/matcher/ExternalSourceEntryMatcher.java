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

public class ExternalSourceEntryMatcher {

    private ExternalSourceEntryMatcher() { }

    public static Matcher<? super Object> matchExternalSourceEntry(String id, String displayValue,
                                                                   String value, String source) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.display", is(displayValue)),
            hasJsonPath("$.value", is(value)),
            hasJsonPath("$.externalSource", is(source))
        );
    }

}
