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

public class PageMatcher {

    private PageMatcher() { }

    public static Matcher<? super Object> pageEntry(int number, int size) {
        return allOf(
            hasJsonPath("$.number", is(number)),
            hasJsonPath("$.size", is(size))
        );
    }

    public static Matcher<? super Object> pageEntryWithTotalPagesAndElements(int number, int size, int totalPages,
                                                                             int totalElements) {
        return allOf(
            hasJsonPath("$.number", is(number)),
            hasJsonPath("$.size", is(size)),
            hasJsonPath("$.totalPages", is(totalPages)),
            hasJsonPath("$.totalElements", is(totalElements))
        );
    }

}
