/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.time.LocalDate;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Plugin interface for the access status calculation.
 */
public interface AccessStatusHelper {
    /**
     * Calculate the access status for the item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @param threshold the embargo threshold date
     * @return an access status value
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getAccessStatusFromItem(Context context, Item item, LocalDate threshold)
        throws SQLException;

    /**
     * Retrieve embargo information for the item
     *
     * @param context the DSpace context
     * @param item the item to check for embargo information
     * @param threshold the embargo threshold date
     * @return an embargo date
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getEmbargoFromItem(Context context, Item item, LocalDate threshold) throws SQLException;

    /**
     * Retrieve the availability date for the bitstream
     *
     * @param context the DSpace context
     * @param bitstream the bitstream to check for embargo information
     * @param threshold the embargo threshold date
     * @return an availability date
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public LocalDate getAvailabilityDateFromBitstream(Context context, Bitstream bitstream, LocalDate threshold)
        throws SQLException;

    /**
     * Look at the DSpace object availability date to determine an access status value.
     *
     * @param availabilityDate the DSpace object availability date
     * @param threshold the embargo threshold date
     * @return an access status value
     */
    public String getAccessStatusFromAvailabilityDate(LocalDate availabilityDate, LocalDate threshold);
}
