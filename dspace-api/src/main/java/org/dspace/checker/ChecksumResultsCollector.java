/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Component that receives BitstreamInfo results from a checker.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public interface ChecksumResultsCollector
{
    /**
     * Collects results.
     * 
     * @param context Context
     * @param info
     *            BitstreamInfo representing the check results.
     * @throws SQLException if database error
     */
    void collect(Context context, MostRecentChecksum info) throws SQLException;
}
