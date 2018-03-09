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
import java.text.DateFormat;
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
import org.dspace.kernel.DSpaceKernel;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempt to parse date strings in a variety of formats.  This uses an external
 * list of regular expressions and associated SimpleDateFormat strings.  Inject
 * the list as pairs of strings using {@link #setPatterns}.  {@link #parse} walks
 * the provided list in the order provided and tries each entry against a String.
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mwood
 */
public class MultiFormatDateParser
{
    private static final Logger log = LoggerFactory.getLogger(MultiFormatDateParser.class);

    /** A list of rules, each binding a regular expression to a date format. */
    private static final ArrayList<Rule> rules = new ArrayList<>();

    private static final TimeZone UTC_ZONE = TimeZone.getTimeZone("UTC");

    /** Format for displaying a result of testing. */
    private static final ThreadLocal<DateFormat> formatter;
    static
    {
        formatter = new ThreadLocal<DateFormat>(){
        @Override
        protected DateFormat initialValue() {
            DateFormat dateTimeInstance = SimpleDateFormat.getDateTimeInstance();
            dateTimeInstance.setTimeZone(UTC_ZONE);
            return dateTimeInstance;
        }
      };
    }

    @Inject
    public void setPatterns(Map<String, String> patterns)
    {
        for (Entry<String, String> rule : patterns.entrySet())
        {
            Pattern pattern;
            try {
                pattern = Pattern.compile(rule.getKey(),Pattern.CASE_INSENSITIVE);
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
            format.setCalendar(Calendar.getInstance(UTC_ZONE));
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

    public static void main(String[] args)
            throws IOException
    {
        DSpaceKernelInit.getKernel(null); // Mainly to initialize Spring
        // TODO direct log to stdout/stderr somehow

        if (args.length > 0) // Test data supplied on the command line
        {
            for (String arg : args)
            {
                testDate(arg);
            }
        }
        else // Get test data from the environment
        {
            String arg;
            if (null == System.console()) // Possibly piped input
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                String line;
                while (null != (line = input.readLine()))
                    testDate(line.trim());
            }
            else // Loop, prompting for input
                while(null != (arg = System.console().readLine("Enter a date-time:  ")))
                {
                    testDate(arg);
                }
        }
    }

    /**
     * Try to parse a date, and report the outcome.
     *
     * @param arg date-time string to be tested.
     */
    private static void testDate(String arg)
    {
        Date result = parse(arg);
        if (null == result)
            System.out.println("Did not match any pattern.");
        else
        {
            System.out.println(formatter.get().format(result));
        }
    }

    /**
     * Holder for a pair:  compiled regex, compiled SimpleDateFormat.
     */
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
