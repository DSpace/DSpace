/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

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

    private static final ArrayList<Rule> rules = new ArrayList<>();

    @Inject
    public void setPatterns(Map<String, String> patterns)
    {
        for (Entry<String, String> rule : patterns.entrySet())
        {
            Pattern pattern;
            try {
                pattern = Pattern.compile(rule.getKey());
            } catch (PatternSyntaxException ex) {
                log.error("Skipping format with unparseable pattern '{}'",
                        rule.getKey());
                continue;
            }

            SimpleDateFormat format;
            try {
            format = new SimpleDateFormat(rule.getValue());
            } catch (IllegalArgumentException ex) {
                log.error("Skipping uninterpretable date format '{}'",
                        rule.getValue());
                continue;
            }
            format.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            format.setLenient(false);

            rules.add(new Rule(pattern, format));
        }
    }

    /**
     * Compare a string to each injected regular expression in entry order, and
     * when it matches, attempt to parse it using the associated format.
     *
     * @param dateString the supposed date to be parsed.
     * @return the result of the first successful parse, or {@code null} if none.
     */
    static public Date parse(String dateString)
    {
        for (Rule candidate : rules)
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

    private static class Rule
    {
        final Pattern pattern;
        final SimpleDateFormat format;
        public Rule(Pattern pattern, SimpleDateFormat format)
        {
            this.pattern = pattern;
            this.format = format;
        }
    }
}
