/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.servicemanager.DSpaceKernelInit;

/**
 * Attempt to parse date strings in a variety of formats.  This uses an external
 * list of regular expressions and associated DateTimeFormatter patterns.  Inject
 * the list as pairs of strings using {@link #setPatterns}.  {@link #parse} walks
 * the provided list in the order provided and tries each entry against a String.
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mwood
 */
public class MultiFormatDateParser {
    private static final Logger log = LogManager.getLogger();

    /**
     * A list of rules, each binding a regular expression to a date format.
     */
    private static final ArrayList<Rule> rules = new ArrayList<>();

    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;

    @Inject
    public void setPatterns(Map<String, String> patterns) {
        for (Entry<String, String> rule : patterns.entrySet()) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(rule.getKey(), Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                log.error("Skipping format with unparsable pattern '{}'",
                        rule::getKey);
                continue;
            }

            DateTimeFormatter format;
            try {
                format = DateTimeFormatter.ofPattern(rule.getValue()).withZone(UTC_ZONE);
            } catch (IllegalArgumentException ex) {
                log.error("Skipping uninterpretable date format '{}'",
                        rule::getValue);
                continue;
            }

            // Determine the granularity of this date rule. Does it represent a year, month, day or a date/time?
            Rule.DateGranularity granularity = getGranularity(rule.getValue());
            rules.add(new Rule(pattern, format, granularity));
        }
    }

    /**
     * Determine the granularity of a date pattern (e.g. YYYY-MM-DD is a "day", but YYYY-MM is a "month")
     * @param datePattern string date pattern.
     * @return DateGranularity value
     */
    private static Rule.DateGranularity getGranularity(String datePattern) {
        Rule.DateGranularity granularity;
        // If the rule contains "HH" symbol (represents Hour) then it is a date/time.
        if (datePattern.contains("HH")) {
            granularity = Rule.DateGranularity.TIME;
        // Else if it contains a "dd" symbol (represents day of month) then it is a date (without a time).
        } else if (datePattern.contains("dd")) {
            granularity = Rule.DateGranularity.DAY;
        // Else if it contains a "MM" system (represents month), then it is a month of the year.
        } else if (datePattern.contains("MM")) {
            granularity = Rule.DateGranularity.MONTH;
        // At this point, we have to assume it's just a year. It has no month or day or hour.
        } else {
            granularity = Rule.DateGranularity.YEAR;
        }
        return granularity;
    }

    /**
     * Compare a string to each injected regular expression in entry order, and
     * when it matches, attempt to parse it using the associated format.
     *
     * @param dateString the supposed date to be parsed.
     * @return the result of the first successful parse, or {@code null} if none.
     */
    static public ZonedDateTime parse(String dateString) {
        for (Rule candidate : rules) {
            if (candidate.pattern.matcher(dateString).matches()) {
                ZonedDateTime result = null;
                try {
                    // Based on the granularity of this date, we need to use different java.time.* classes to parse it.
                    switch (candidate.granularity) {
                        case TIME:
                            result = ZonedDateTime.parse(dateString, candidate.format);
                            break;
                        case DAY:
                            // Assume start of day (in UTC time zone)
                            result = LocalDate.parse(dateString, candidate.format)
                                              .atStartOfDay(ZoneOffset.UTC);
                            break;
                        case MONTH:
                            // Assume start of first day of month (in UTC timezone)
                            result = YearMonth.parse(dateString, candidate.format)
                                              .atDay(1).atStartOfDay(ZoneOffset.UTC);
                            break;
                        case YEAR:
                            result = Year.parse(dateString, candidate.format)
                                         .atMonth(1).atDay(1).atStartOfDay(ZoneOffset.UTC);
                            break;
                        default:
                            // Should not occur. If it does, this will be caught & logged by the catch() below.
                            throw new DateTimeException("Could not find a valid parser for this matched pattern.");
                    }
                } catch (DateTimeParseException ex) {
                    log.info("Date string '{}' matched pattern '{}' but did not parse:  {}",
                        () -> dateString, candidate.format::toString, ex::getMessage);
                    continue;
                }
                return result;
            }
        }

        return null;
    }

    public static void main(String[] args)
        throws IOException {
        DSpaceKernelInit.getKernel(null); // Mainly to initialize Spring
        // TODO direct log to stdout/stderr somehow

        // Test data supplied on the command line
        if (args.length > 0) {
            for (String arg : args) {
                testDate(arg);
            }
        } else {
            // Else, get test data from the environment
            String arg;
            // Possibly piped input
            if (null == System.console()) {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                String line;
                while (null != (line = input.readLine())) {
                    testDate(line.trim());
                }
            } else {
                // Loop, prompting for input
                while (null != (arg = System.console().readLine("Enter a date-time:  "))) {
                    testDate(arg);
                }
            }
        }
    }

    /**
     * Try to parse a date, and report the outcome.
     *
     * @param arg date-time string to be tested.
     */
    private static void testDate(String arg) {
        ZonedDateTime result = parse(arg);
        if (null == result) {
            System.out.println("Did not match any pattern.");
        } else {
            // NOTE: This formats dates as an instant in UTC (e.g. '2011-12-03T10:15:30Z')
            System.out.println(result.format(DateTimeFormatter.ISO_INSTANT));
        }
    }

    /**
     * Holder for a pair:  compiled regex, compiled DateTimeFormatter.
     */
    private static class Rule {
        enum DateGranularity { YEAR, MONTH, DAY, TIME }
        final Pattern pattern;
        final DateTimeFormatter format;
        final DateGranularity granularity;

        public Rule(Pattern pattern, DateTimeFormatter format, DateGranularity granularity) {
            this.pattern = pattern;
            this.format = format;
            this.granularity = granularity;
        }
    }
}
