/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Hamcrest Matcher for comparing a Date with an ISO 8601 zoned string form
 * of a date.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class DateMatcher
        extends BaseMatcher<String> {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    private final ZonedDateTime matchDate;

    /**
     * Create a matcher for a given Date.
     * @param matchDate The date that tested values should match.
     */
    public DateMatcher(ZonedDateTime matchDate) {
        // Truncate to seconds. We aren't matching with millisecond precision because doing so may result
        // in random failures if one of the two dates is rounded differently.
        this.matchDate = (matchDate != null ? matchDate.truncatedTo(ChronoUnit.SECONDS) : null);
    }

    @Override
    public boolean matches(Object testDate) {
        // null : null is a match
        if (null == matchDate && null == testDate) {
            return true;
        }

        // Null matchDate never matches non-null testDate
        if (null == matchDate) {
            return false;
        }

        // We only match strings here
        if (!(testDate instanceof String)) {
            throw new IllegalArgumentException("Argument not a String");
        }

        // Decode the string to a Date, truncated to seconds.
        ZonedDateTime testDateDecoded;
        try {
            testDateDecoded = ZonedDateTime.parse((String) testDate, dateFormat).truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Argument '" + testDate
                    + "' is not an ISO 8601 zoned date", ex);
        }

        // Compare with the Date that must match
        return matchDate.equals(testDateDecoded);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is the same date as ");
        description.appendText(matchDate != null ? dateFormat.format(matchDate) : "null");
    }

    /**
     * Return a Matcher for a given Date.
     * @param matchDate the date which tested values should match.
     * @return a new Matcher for matchDate.
     */
    static public DateMatcher dateMatcher(ZonedDateTime matchDate) {
        return new DateMatcher(matchDate);
    }
}
