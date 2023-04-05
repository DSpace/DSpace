/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.core.Context;

/**
 * The Service responsible for processing urls
 */
public interface OpenUrlService {
    /**
     * Process the url
     * @param c - the context
     * @param urlStr - the url to be processed
     * @throws IOException
     * @throws SQLException
     */
    void processUrl(Context c, String urlStr) throws SQLException;

    /**
     * Will process all urls stored in the database and try contacting them again
     * @param context
     * @throws SQLException
     */
    void reprocessFailedQueue(Context context) throws SQLException;

    /**
     * Will log the failed url in the database
     * @param context
     * @param url
     * @throws SQLException
     */
    void logfailed(Context context, String url) throws SQLException;


}
