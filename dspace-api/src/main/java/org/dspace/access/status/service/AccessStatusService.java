/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Public interface to the access status subsystem.
 * <p>
 * Configuration properties: (with examples)
 * {@code
 * # values for the forever embargo date threshold
 * # This threshold date is used in the default access status helper to dermine if an item is
 * # restricted or embargoed based on the start date of the primary (or first) file policies.
 * # In this case, if the policy start date is inferior to the threshold date, the status will
 * # be embargo, else it will be restricted.
 * # You might want to change this threshold based on your needs. For example: some databases
 * # doesn't accept a date superior to 31 december 9999.
 * access.status.embargo.forever.year = 10000
 * access.status.embargo.forever.month = 1
 * access.status.embargo.forever.day = 1
 * # implementation of access status helper plugin - replace with local implementation if applicable
 * # This default access status helper provides an item status based on the policies of the primary
 * # bitstream (or first bitstream in the original bundles if no primary file is specified).
 * plugin.single.org.dspace.access.status.AccessStatusHelper = org.dspace.access.status.DefaultAccessStatusHelper
 * }
 */
public interface AccessStatusService {

    /**
     * Calculate the access status for an Item while considering the forever embargo date threshold.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return an access status value
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getAccessStatus(Context context, Item item) throws SQLException;

    /**
     * Retrieve embargo information for the item
     *
     * @param context the DSpace context
     * @param item the item to check for embargo information
     * @return an embargo date
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getEmbargoFromItem(Context context, Item item) throws SQLException;
}
