/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.hamcrest.Matcher;

/**
 * Helper class to simplify testing of the submission section configuration.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class SubmissionSectionMatcher {

    private SubmissionSectionMatcher() {

    }

    public static Matcher<? super Object> matches(String id, boolean mandatory, String sectionType) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.mandatory", is(mandatory)),
            hasJsonPath("$.sectionType", is(sectionType)),
            hasJsonPath("$.type", is("submissionsection")),
            hasNoJsonPath("$.visibility"));
    }

    public static Matcher<? super Object> matches(String id, boolean mandatory, String sectionType,
        Map<String, String> visibilities) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.mandatory", is(mandatory)),
            hasJsonPath("$.sectionType", is(sectionType)),
            hasJsonPath("$.type", is("submissionsection")),
            hasJsonPath("$.visibility", is(visibilities)));
    }
}
