/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a submission definition
 */
public class SubmissionDefinitionsMatcher {

    private SubmissionDefinitionsMatcher() { }

    public static Matcher<Object> matchSubmissionDefinition(boolean isDefault, String name, String id) {
        return allOf(
                matchProperties(isDefault, name, id),
                matchLinks()
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "collections[]",
                "sections"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks() {
        return HalMatcher.matchLinks(REST_SERVER_URL + "config/submissiondefinitions/traditional",
                "collections",
                "sections",
                "self"
        );
    }

    public static Matcher<? super Object> matchProperties(boolean isDefault, String name, String id) {
        return allOf(
                hasJsonPath("$.isDefault", is(isDefault)),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.type", is("submissiondefinition")),
                hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "config/submissiondefinitions/" + id)),
                hasJsonPath("$._links.sections.href",
                        is(REST_SERVER_URL + "config/submissiondefinitions/" + id + "/sections"))
        );
    }
}
