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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class SearchFilterMatcher {

    private SearchFilterMatcher() { }

    public static Matcher<? super Object> titleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("title")),
                hasJsonPath("$.hasFacets", is(false)),
                hasJsonPath("$.type", is("text")),
                hasJsonPath("$.openByDefault", is(true)),
                checkOperators()

        );
    }

    public static Matcher<? super Object> authorFilter() {
        return allOf(
                hasJsonPath("$.filter", is("author")),
                hasJsonPath("$.hasFacets", is(true)),
                hasJsonPath("$.type", is("text")),
                hasJsonPath("$.openByDefault", is(true)),
                checkOperators()

        );
    }

    public static Matcher<? super Object> subjectFilter() {
        return allOf(
                hasJsonPath("$.filter", is("subject")),
                hasJsonPath("$.hasFacets", is(true)),
                hasJsonPath("$.type", is("hierarchical")),
                hasJsonPath("$.openByDefault", is(false)),
                checkOperators()

        );
    }

    public static Matcher<? super Object> dateIssuedFilter() {
        return allOf(
                hasJsonPath("$.filter", is("dateIssued")),
                hasJsonPath("$.hasFacets", is(true)),
                hasJsonPath("$.type", is("date")),
                hasJsonPath("$.openByDefault", is(false)),
                checkOperators()

        );
    }

    public static Matcher<? super Object> hasContentInOriginalBundleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("has_content_in_original_bundle")),
                hasJsonPath("$.hasFacets", is(true)),
                hasJsonPath("$.type", is("standard")),
                hasJsonPath("$.openByDefault", is(false)),
                checkOperators()

        );
    }

    public static Matcher<? super Object> hasFileNameInOriginalBundleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("original_bundle_filenames")),
                checkOperators()

        );
    }

    public static Matcher<? super Object> hasFileDescriptionInOriginalBundleFilter() {
        return allOf(
                hasJsonPath("$.filter", is("original_bundle_descriptions")),
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
                        hasJsonPath("$.operator", is("notcontains")),
                        hasJsonPath("$.operator", is("query"))
                        ))
        );
    }
    public static Matcher<? super Object> entityTypeFilter() {
        return allOf(
            hasJsonPath("$.filter", is("entityType")),
            hasJsonPath("$.hasFacets", is(true)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
    public static Matcher<? super Object> isAuthorOfPublicationRelation() {
        return allOf(
            hasJsonPath("$.filter", is("isAuthorOfPublication")),
            hasJsonPath("$.hasFacets", is(false)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
    public static Matcher<? super Object> isProjectOfPublicationRelation() {
        return allOf(
            hasJsonPath("$.filter", is("isProjectOfPublication")),
            hasJsonPath("$.hasFacets", is(false)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
    public static Matcher<? super Object> isOrgUnitOfPublicationRelation() {
        return allOf(
            hasJsonPath("$.filter", is("isOrgUnitOfPublication")),
            hasJsonPath("$.hasFacets", is(false)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
    public static Matcher<? super Object> isPublicationOfJournalIssueRelation() {
        return allOf(
            hasJsonPath("$.filter", is("isPublicationOfJournalIssue")),
            hasJsonPath("$.hasFacets", is(false)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
    public static Matcher<? super Object> isJournalOfPublicationRelation() {
        return allOf(
            hasJsonPath("$.filter", is("isJournalOfPublication")),
            hasJsonPath("$.hasFacets", is(false)),
            hasJsonPath("$.type", is("text")),
            hasJsonPath("$.openByDefault", is(false)),
            checkOperators()
        );
    }
}
