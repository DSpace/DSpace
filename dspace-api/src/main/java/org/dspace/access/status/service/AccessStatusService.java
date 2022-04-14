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
 * access.status.embargo.forever.year = 10000
 * access.status.embargo.forever.month = 1
 * access.status.embargo.forever.day = 1
 * # implementation of access status builder plugin - replace with local implementation if applicable
 * plugin.single.org.dspace.access.status.AccessStatusBuilder = org.dspace.access.status.DefaultAccessStatusBuilder
 * }
 */
public interface AccessStatusService {

    /**
     * Calculate the access status for an Item while considering the forever embargo date threshold.
     *
     * @param context the DSpace context
     * @param item    the item
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getAccessStatus(Context context, Item item) throws SQLException;
}
