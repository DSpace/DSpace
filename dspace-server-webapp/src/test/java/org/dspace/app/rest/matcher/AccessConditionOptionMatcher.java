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

/**
 * Provide convenient org.hamcrest.Matcher to verify
 * a SubmissionAccessOptionRest json response.
 *
 * @author Mykhaylo Boychuk (mykhaylob.oychuk at 4science.com)
 */
public class AccessConditionOptionMatcher {

    private AccessConditionOptionMatcher() {}

    public static Matcher<? super Object> matchAccessConditionOption(String name, String groupName,
            boolean hasStartDate, boolean hasEndDate, String startDateLimit, String endDateLimit) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.groupName", is(groupName)),
                hasJsonPath("$.hasStartDate", is(hasStartDate)),
                hasJsonPath("$.hasEndDate", is(hasEndDate)),
                hasJsonPath("$.startDateLimit", is(startDateLimit)),
                hasJsonPath("$.endDateLimit", is(endDateLimit))
                );
    }
}