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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class SubmissionFormFieldMatcher {

    private SubmissionFormFieldMatcher() { }

    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String mandatoryMessage,
            boolean repeatable, String hints, String metadata) {
        return matchFormFieldDefinition(type, label, mandatoryMessage, repeatable, hints, null, metadata);
    }

    public static Matcher<? super Object> matchFormFieldDefinition(String type, String label, String mandatoryMessage,
            boolean repeatable, String hints, String style, String metadata) {
        return allOf(
                // check each field definition
            hasJsonPath("$.input.type", is(type)),
            hasJsonPath("$.label", containsString(label)),
            hasJsonPath("$.selectableMetadata[0].metadata", is(metadata)),
            mandatoryMessage != null ? hasJsonPath("$.mandatoryMessage", containsString(mandatoryMessage)) :
                hasNoJsonPath("$.mandatoryMessage"),
            hasJsonPath("$.mandatory", is(mandatoryMessage != null)),
            hasJsonPath("$.repeatable", is(repeatable)),
            style != null ? hasJsonPath("$.style", is(style)) :
                hasNoJsonPath("$.style"),
            hasJsonPath("$.hints", containsString(hints))
        );
    }
}
