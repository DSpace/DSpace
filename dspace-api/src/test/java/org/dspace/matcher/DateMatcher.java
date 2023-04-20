/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matcher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private static final SimpleDateFormat dateFormat
            = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private final Date matchDate;

    /**
     * Create a matcher for a given Date.
     * @param matchDate The date that tested values should match.
     */
    public DateMatcher(Date matchDate) {
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
        Date testDateDecoded;
        try {
            testDateDecoded = dateFormat.parse((String)testDate);
        } catch (ParseException ex) {
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
    static public DateMatcher dateMatcher(Date matchDate) {
        return new DateMatcher(matchDate);
    }
}
