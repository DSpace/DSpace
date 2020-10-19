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

public class NBEventMatcher {

    private NBEventMatcher() { }

    public static Matcher<? super Object> matchNBEventEntry(String id, String originalId,String itemId,
        String handle) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.originalId", is(originalId)),
            hasJsonPath("$.itemId", is(itemId)),
            hasJsonPath("$.status", is("PENDING")),
            hasJsonPath("$.handle", is(handle))
        );
    }
}
