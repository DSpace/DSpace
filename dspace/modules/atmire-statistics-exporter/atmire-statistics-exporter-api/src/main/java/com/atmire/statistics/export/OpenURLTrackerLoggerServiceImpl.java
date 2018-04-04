package com.atmire.statistics.export;

import com.atmire.statistics.export.dao.OpenURLTrackerDAO;
import com.atmire.statistics.export.service.OpenURLTrackerLoggerService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public class OpenURLTrackerLoggerServiceImpl implements OpenURLTrackerLoggerService {

    @Autowired(required = true)
    protected OpenURLTrackerDAO openURLTrackerDAO;

    @Override
    public void remove(Context context, OpenURLTracker openURLTracker) throws SQLException {
        openURLTrackerDAO.delete(context,openURLTracker);
    }

    @Override
    public List<OpenURLTracker> findAll(Context context) throws SQLException {
        return openURLTrackerDAO.findAll(context,OpenURLTracker.class);
    }

    @Override
    public OpenURLTracker create(Context context) throws SQLException {
        OpenURLTracker openURLTracker = openURLTrackerDAO.create(context, new OpenURLTracker());
        return openURLTracker;
    }
}
