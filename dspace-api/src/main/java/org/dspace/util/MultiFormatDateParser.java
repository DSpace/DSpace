/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempt to parse date strings in a variety of formats.  This uses an external
 * list of regular expressions and associated SimpleDateFormat strings.  Supply
 * such rules as the file {@code dateFormats.csv} on the class path.
 * {@link parse} walks the provided list in order and tries each entry against a
 * String.  The first complete match selects the format to be used.
 *
 * Dates are parsed as being in the UTC zone.
 *
 * @author mwood
 */
public class MultiFormatDateParser
{
    private static final Logger log = LoggerFactory.getLogger(MultiFormatDateParser.class);

    private static final String DSPACE_DIR = "dspace.dir";

    private static final String FORMATS_FILE = "config/dateFormats.csv";

    private static ArrayList<Rule> rules;

    /**
     * Read pairs of [regex, format] strings from a CSV file on the class path
     * into the rule table.
     */
    private static void loadPatterns()
    {
        rules = new ArrayList<>();

        String dspaceDir = new DSpace().getConfigurationService().getProperty(DSPACE_DIR);
        File formatsFile = new File(dspaceDir, FORMATS_FILE);
        if (!formatsFile.isFile())
        {
            log.error("Date formats list {} not found",
                    formatsFile.getAbsolutePath());
            return;
        }

        try (CSVReader reader = new CSVReader(new FileReader(formatsFile)))
        {
            while (true)
            {
                String[] rule;
                rule = reader.readNext();
                if (null == rule) // if there is no more input
                {
                    break;
                }
                if (rule.length < 2)
                {
                    log.error("Skipping a rule with too few fields");
                    continue;
                }

                Pattern pattern;
                try {
                    pattern = Pattern.compile(rule[0]);
                } catch (PatternSyntaxException ex) {
                    log.error("Skipping format with unparseable regex pattern '{}'",
                            rule[0]);
                    continue;
                }

                SimpleDateFormat format;
                try {
                    format = new SimpleDateFormat(rule[1]);
                } catch (IllegalArgumentException ex) {
                    log.error("Skipping uninterpretable date format '{}'",
                            rule[1]);
                    continue;
                }
                format.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                format.setLenient(false);

                rules.add(new Rule(pattern, format));
            }
        } catch (IOException ex) {
            log.error("Aborted rule loading", ex);
        }
    }

    /**
     * Compare a string to each configured regular expression in entry order,
     * and when it matches, attempt to parse it using the associated format.
     *
     * @param dateString the supposed date to be parsed.
     * @return the result of the first successful parse, or {@code null} if none.
     */
    static public Date parse(String dateString)
    {
        if (null == rules)
            loadPatterns();

        for (Rule rule : rules)
        {
            if (rule.pattern.matcher(dateString).matches())
            {
                Date result;
                try {
                    synchronized(rule.format) {
                        result = rule.format.parse(dateString);
                    }
                } catch (ParseException ex) {
                    log.info("Date string '{}' matched pattern '{}' but did not parse:  {}",
                            new String[] {dateString, rule.format.toPattern(),
                                ex.getMessage()});
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
