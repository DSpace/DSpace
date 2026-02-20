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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.util.DateMathParser;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
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
                StringUtils.isNotBlank(maxStartDate) ? hasJsonPath("$.maxStartDate", new DateMathMatcher(maxStartDate))
                                                     : hasNoJsonPath("$.maxStartDate"),
                StringUtils.isNotBlank(maxEndDate) ? hasJsonPath("$.maxEndDate", new DateMathMatcher(maxEndDate))
                                                     : hasNoJsonPath("$.maxEndDate")
                );
    }

    /**
     * Internal matcher to compare an ISO date from JSON with a DateMath expression.
     */
    private static class DateMathMatcher extends BaseMatcher<String> {
        private final String mathExpression;
        private final LocalDate expectedDate;

        public DateMathMatcher(String mathExpression) {
            this.mathExpression = mathExpression;
            try {
                DateMathParser dmp = new DateMathParser();
                LocalDateTime calculated = dmp.parseMath(mathExpression);
                this.expectedDate = calculated.toLocalDate();
            } catch (Exception e) {
                throw new RuntimeException("Error calculating DateMath in Matcher: " + mathExpression, e);
            }
        }

        @Override
        public boolean matches(Object item) {
            if (!(item instanceof String)) {
                return false;
            }
            String dateStr = (String) item;
            try {
                LocalDate actualDate;

                if (dateStr.length() > 10) {
                    actualDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
                } else {
                    actualDate = LocalDate.parse(dateStr);
                }
                return expectedDate.equals(actualDate);
            } catch (DateTimeParseException e) {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("A date matching expression'" + mathExpression + "' (" + expectedDate + ")");
        }
    }
}