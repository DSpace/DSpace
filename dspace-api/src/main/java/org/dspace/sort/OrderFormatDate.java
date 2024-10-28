/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Standard date ordering delegate implementation. The only "special" need is
 * to treat dates with less than 4-digit year.
 *
 * @author Andrea Bollini
 */
public class OrderFormatDate implements OrderFormatDelegate {
    private final SimpleDateFormat yearIso = new SimpleDateFormat("yyyy");

    @Override
    public String makeSortString(String value, String language) {
        int padding = 0;
        int endYearIdx = value.indexOf('-');

        if (endYearIdx >= 0 && endYearIdx < 4) {
            padding = 4 - endYearIdx;
        } else if (value.length() < 4) {
            padding = 4 - value.length();
        }

        String newValue = value;
        if (padding > 0) {
            // padding the value from left with 0 so that 87 -> 0087, 687-11-24
            // -> 0687-11-24
            newValue = String.format("%1$0" + padding + "d", 0)
                + value;
        }

        if (isValidDate(newValue)) {
            return newValue;
        } else {
            return null;
        }
    }

    private boolean isValidDate(String value) {
        try {
            yearIso.parse(value);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
