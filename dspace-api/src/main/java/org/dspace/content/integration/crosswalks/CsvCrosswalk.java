/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Implementation of {@StreamDisseminationCrosswalk} to produce a csv file starting from a template.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CsvCrosswalk extends TabularCrosswalk {

    private static final String CSV_QUOTE = String.valueOf('"');

    private static final String CSV_ESCAPED_QUOTE_STR = CSV_QUOTE + CSV_QUOTE;

    @Override
    public String getMIMEType() {
        return "text/csv";
    }

    protected void writeRows(List<List<String>> rows, OutputStream out) {
        try (PrintWriter writer = new PrintWriter(out)) {
            for (List<String> row : rows) {
                writer.write(String.join(getFieldsSeparator(), row));
                writer.write("\n");
            }
        }
    }

    protected String getValuesSeparator() {
        return configurationService.getProperty("crosswalk.csv.separator.values", "||");
    }

    protected String getNestedValuesSeparator() {
        return configurationService.getProperty("crosswalk.csv.separator.nested-values", "||");
    }

    protected String getInsideNestedSeparator() {
        return configurationService.getProperty("crosswalk.csv.separator.inside-nested", "/");
    }

    protected String escapeValue(String value) {
        return CSV_QUOTE + value.replaceAll(CSV_QUOTE, CSV_ESCAPED_QUOTE_STR) + CSV_QUOTE;
    }

    private String getFieldsSeparator() {
        return configurationService.getProperty("crosswalk.csv.separator.fields", ",");
    }

}
