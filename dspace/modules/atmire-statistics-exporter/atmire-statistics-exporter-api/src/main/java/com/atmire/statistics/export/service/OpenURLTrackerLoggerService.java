package com.atmire.statistics.export.service;

import com.atmire.statistics.export.OpenURLTracker;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public interface OpenURLTrackerLoggerService {

    void remove(Context context, OpenURLTracker openURLTracker) throws SQLException;

    List<OpenURLTracker> findAll(Context context) throws SQLException;

    OpenURLTracker create(Context context) throws SQLException;
}
