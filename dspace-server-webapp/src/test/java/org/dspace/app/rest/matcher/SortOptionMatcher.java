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

public class SortOptionMatcher {

    private SortOptionMatcher() { }

    public static Matcher<? super Object> titleSortOption() {
        return allOf(
            hasJsonPath("$.name", is("dc.title"))
        );
    }

    public static Matcher<? super Object> dateIssuedSortOption() {
        return allOf(
            hasJsonPath("$.name", is("dc.date.issued"))
        );
    }

    public static Matcher<? super Object> dateAccessionedSortOption() {
        return allOf(
                hasJsonPath("$.name", is("dc.date.accessioned"))
        );
    }

    public static Matcher<? super Object> scoreSortOption() {
        return allOf(
            hasJsonPath("$.name", is("score"))
        );
    }

    public static Matcher<? super Object> sortByAndOrder(String by, String order) {
        return allOf(
            hasJsonPath("$.by", is(by)),
            hasJsonPath("$.order", is(order))
        );
    }

}
