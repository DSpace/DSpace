/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.service;

import org.dspace.app.util.WebApp;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Service interface class for the WebApp object.
 * The implementation of this class is responsible for all business logic calls for the WebApp object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WebAppService {

    public WebApp create(Context context, String appName, String url, Date started, int isUI) throws SQLException;

    public List<WebApp> findAll(Context context) throws SQLException;

    public void delete(Context context, WebApp webApp) throws SQLException;

    /**
     *
     * @return Return the list of running applications.
     */
    public List<WebApp> getApps();
}
