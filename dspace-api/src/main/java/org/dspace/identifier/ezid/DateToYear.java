/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.ezid;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Convert a date-time string to the year thereof.
 *
 * @author mwood
 */
public class DateToYear
    implements Transform {

    @Override
    public synchronized String transform(String from)
        throws DateTimeParseException {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate parsedDate = LocalDate.parse(from, formatter);
        return String.valueOf(parsedDate.getYear());
    }
}
