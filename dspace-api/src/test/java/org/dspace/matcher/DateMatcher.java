/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    private static final DateTimeFormatter dateFormat
            = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private final LocalDateTime matchDate;

    /**
     * Create a matcher for a given Date.
     * @param matchDate The date that tested values should match.
     */
    public DateMatcher(LocalDateTime matchDate) {
        this.matchDate = matchDate;
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

        // Decode the string to a Date
        LocalDateTime testDateDecoded;
        try {
            testDateDecoded = LocalDateTime.parse((String) testDate, dateFormat);
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
        description.appendText(dateFormat.format(matchDate));
    }

    /**
     * Return a Matcher for a given Date.
     * @param matchDate the date which tested values should match.
     * @return a new Matcher for matchDate.
     */
    static public DateMatcher dateMatcher(LocalDateTime matchDate) {
        return new DateMatcher(matchDate);
    }
}
