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
import static org.hamcrest.Matchers.notNullValue;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matcher;

/**
 * Provide convenient org.hamcrest.Matcher to verify
 * a SubmissionAccessOptionRest json response.
 *
 * @author Mykhaylo Boychuk (mykhaylob.oychuk at 4science.com)
 */
public class AccessConditionOptionMatcher {

    private AccessConditionOptionMatcher() {}

    public static Matcher<? super Object> matchAccessConditionOption(String name,
            Boolean hasStartDate, Boolean hasEndDate, String maxStartDate, String maxEndDate) {
        return allOf(
                hasJsonPath("$.name", is(name)),
                Objects.nonNull(hasStartDate) ? hasJsonPath("$.hasStartDate", is(hasStartDate))
                                              : hasNoJsonPath("$.hasStartDate"),
                Objects.nonNull(hasEndDate) ? hasJsonPath("$.hasEndDate", is(hasEndDate))
                                            : hasNoJsonPath("$.hasEndDate"),
                StringUtils.isNotBlank(maxStartDate) ? hasJsonPath("$.maxStartDate", notNullValue())
                                                     : hasNoJsonPath("$.maxStartDate"),
                StringUtils.isNotBlank(maxEndDate) ? hasJsonPath("$.maxEndDate", notNullValue())
                                                     : hasNoJsonPath("$.maxEndDate")
                );
    }

}