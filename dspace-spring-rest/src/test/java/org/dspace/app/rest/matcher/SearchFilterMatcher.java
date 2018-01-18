/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;

public class SearchFilterMatcher {
    public SearchFilterMatcher(){

    }
    public static Matcher<? super Object> titleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("title")),
                checkOperators()

        );
    }
    public static Matcher<? super Object> authorFilter() {
        return allOf(
                hasJsonPath("$.filter", is("author")),
                checkOperators()

        );
    }
    public static Matcher<? super Object> subjectFilter() {
        return allOf(
                hasJsonPath("$.filter", is("subject")),
                checkOperators()

        );
    }
    public static Matcher<? super Object> dateIssuedFilter() {
        return allOf(
                hasJsonPath("$.filter", is("dateIssued")),
                checkOperators()

        );
    }
    public static Matcher<? super Object> hasContentInOriginalBundleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("has_content_in_original_bundle")),
                checkOperators()

        );
    }

    public static Matcher<? super Object> checkOperators() {
        return allOf(
                hasJsonPath("$.operators",  containsInAnyOrder(
                        hasJsonPath("$.operator", is("equals")),
                        hasJsonPath("$.operator", is("notequals")),
                        hasJsonPath("$.operator", is("authority")),
                        hasJsonPath("$.operator", is("notauthority")),
                        hasJsonPath("$.operator", is("contains")),
                        hasJsonPath("$.operator", is("notcontains"))
                        ))
        );
    }
}
