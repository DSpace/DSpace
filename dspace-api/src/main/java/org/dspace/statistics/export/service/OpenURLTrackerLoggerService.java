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
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public interface OpenURLTrackerLoggerService {

    void remove(Context context, OpenURLTracker openURLTracker) throws SQLException;

    List<OpenURLTracker> findAll(Context context) throws SQLException;

    OpenURLTracker create(Context context) throws SQLException;
}
