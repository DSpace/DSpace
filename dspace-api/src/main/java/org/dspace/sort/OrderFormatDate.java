/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import java.util.Date;

import org.dspace.util.MultiFormatDateParser;

/**
 * Standard date ordering delegate implementation using date format
 * parsing from o.d.u.MultiFormatDateParser.
 *
 * @author Andrea Bollini
 * @author Alan Orth
 */
public class OrderFormatDate implements OrderFormatDelegate {
    @Override
    public String makeSortString(String value, String language) {
        Date result = MultiFormatDateParser.parse(value);

        // If parsing was successful we return the value as an ISO instant,
        // otherwise we return null so Solr does not index this date value.
        if (result != null) {
            return result.toInstant().toString();
        } else {
            return null;
        }
    }
}
