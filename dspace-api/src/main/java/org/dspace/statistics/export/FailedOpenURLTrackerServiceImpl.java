/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.statistics.export.dao.OpenURLTrackerDAO;
import org.dspace.statistics.export.service.FailedOpenURLTrackerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the service that handles the OpenURLTracker database operations
 */
public class FailedOpenURLTrackerServiceImpl implements FailedOpenURLTrackerService {

    @Autowired(required = true)
    protected OpenURLTrackerDAO openURLTrackerDAO;

    /**
     * Removes an OpenURLTracker from the database
     * @param context
     * @param openURLTracker
     * @throws SQLException
     */
    @Override
    public void remove(Context context, OpenURLTracker openURLTracker) throws SQLException {
        openURLTrackerDAO.delete(context, openURLTracker);
    }

    /**
     * Returns all OpenURLTrackers from the database
     * @param context
     * @return all OpenURLTrackers
     * @throws SQLException
     */
    @Override
    public List<OpenURLTracker> findAll(Context context) throws SQLException {
        return openURLTrackerDAO.findAll(context, OpenURLTracker.class);
    }

    /**
     * Creates a new OpenURLTracker
     * @param context
     * @return the created OpenURLTracker
     * @throws SQLException
     */
    @Override
    public OpenURLTracker create(Context context) throws SQLException {
        OpenURLTracker openURLTracker = openURLTrackerDAO.create(context, new OpenURLTracker());
        return openURLTracker;
    }
}
