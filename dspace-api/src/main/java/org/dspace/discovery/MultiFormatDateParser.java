/*
 * Copyright 2014 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Dec 12, 2014
 */

package org.dspace.discovery;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempt to parse date strings in a variety of formats.  This uses an external
 * list of regular expressions and associated SimpleDateFormat strings.  Inject
 * the list as pairs of strings using {@link setPatterns}.  {@link parse} walks
 * the provided list in the order provided and tries each entry against a String.
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mwood
 */
public class MultiFormatDateParser
{
    private static final Logger log = LoggerFactory.getLogger(MultiFormatDateParser.class);

    private static final ArrayList<Candidate> candidates = new ArrayList<>();

    @Inject
    private static void setPatterns(Map<String, String> patterns)
    {
        for (Entry<String, String> candidate : patterns.entrySet())
        {
            Pattern pattern;
            try {
                pattern = Pattern.compile(candidate.getKey());
            } catch (PatternSyntaxException ex) {
                log.error("Skipping format with unparseable pattern '{}'",
                        candidate.getKey());
                continue;
            }

            SimpleDateFormat format;
            try {
            format = new SimpleDateFormat(candidate.getValue());
            } catch (IllegalArgumentException ex) {
                log.error("Skipping uninterpretable date format '{}'",
                        candidate.getValue());
                continue;
            }
            format.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            format.setLenient(false);

            candidates.add(new Candidate(pattern, format));
        }
    }

    /**
     * Compare a string to each injected regular expression in entry order, and
     * when it matches, attempt to parse it using the associated format.
     *
     * @param dateString the supposed date to be parsed.
     * @return the result of the first successful parse, or {@code null} if none.
     */
    static Date parse(String dateString)
    {
        for (Candidate candidate : candidates)
        {
            if (candidate.pattern.matcher(dateString).matches())
            {
                Date result;
                try {
                    synchronized(candidate.format) {
                        result = candidate.format.parse(dateString);
                    }
                } catch (ParseException ex) {
                    log.info("Date string '{}' matched pattern '{}' but did not parse:  {}",
                            new String[] {dateString, candidate.format.toPattern(), ex.getMessage()});
                    continue;
                }
                return result;
            }
        }

        return null;
    }

    private static class Candidate
    {
        final Pattern pattern;
        final SimpleDateFormat format;
        public Candidate(Pattern pattern, SimpleDateFormat format)
        {
            this.pattern = pattern;
            this.format = format;
        }
    }
}
