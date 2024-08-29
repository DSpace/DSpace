/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.statistics.export.OpenURLTracker;

/**
 * Interface of the service that handles the OpenURLTracker database operations
 */
public interface FailedOpenURLTrackerService {

    /**
     * Removes an OpenURLTracker from the database
     * @param context
     * @param openURLTracker
     * @throws SQLException
     */
    void remove(Context context, OpenURLTracker openURLTracker) throws SQLException;

    /**
     * Returns all OpenURLTrackers from the database
     * @param context
     * @return all OpenURLTrackers
     * @throws SQLException
     */
    List<OpenURLTracker> findAll(Context context) throws SQLException;

    /**
     * Creates a new OpenURLTracker
     * @param context
     * @return the created OpenURLTracker
     * @throws SQLException
     */
    OpenURLTracker create(Context context) throws SQLException;
}
