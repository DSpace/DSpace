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
import org.dspace.statistics.export.service.OpenURLTrackerLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public class OpenURLTrackerLoggerServiceImpl implements OpenURLTrackerLoggerService {

    @Autowired(required = true)
    protected OpenURLTrackerDAO openURLTrackerDAO;

    @Override
    public void remove(Context context, OpenURLTracker openURLTracker) throws SQLException {
        openURLTrackerDAO.delete(context, openURLTracker);
    }

    @Override
    public List<OpenURLTracker> findAll(Context context) throws SQLException {
        return openURLTrackerDAO.findAll(context, OpenURLTracker.class);
    }

    @Override
    public OpenURLTracker create(Context context) throws SQLException {
        OpenURLTracker openURLTracker = openURLTrackerDAO.create(context, new OpenURLTracker());
        return openURLTracker;
    }
}
