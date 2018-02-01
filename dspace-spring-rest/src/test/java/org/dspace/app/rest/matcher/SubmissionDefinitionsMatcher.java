/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Utility class to construct a Matcher for a submission definition
 */
public class SubmissionDefinitionsMatcher {

    public SubmissionDefinitionsMatcher() {

    }

	public static Matcher<Object> matchSubmissionDefinition(boolean isDefault, String name, String id) {
		return allOf(
		        hasJsonPath("$.isDefault", is(isDefault)),
		        hasJsonPath("$.name", is(name)),
		        hasJsonPath("$.id", is(id)),
		        hasJsonPath("$.type", is("submissiondefinition")),
		        hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "config/submissiondefinitions/" + id)),
		        hasJsonPath("$._links.sections.href", is(REST_SERVER_URL + "config/submissiondefinitions/" + id + "/sections"))
		);
	}
}