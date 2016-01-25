/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.app.util.dao.WebAppDAO;
import org.dspace.app.util.service.WebAppService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for the WebApp object.
 * This class is responsible for all business logic calls for the WebApp object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WebAppServiceImpl implements WebAppService {

    private final Logger log = Logger.getLogger(WebAppServiceImpl.class);

    @Autowired(required = true)
    protected WebAppDAO webAppDAO;


    protected WebAppServiceImpl()
    {

    }

    @Override
    public WebApp create(Context context, String appName, String url, Date started, int isUI) throws SQLException {
        WebApp webApp = webAppDAO.create(context, new WebApp());
        webApp.setAppName(appName);
        webApp.setUrl(url);
        webApp.setStarted(started);
        webApp.setIsui(isUI);
        webAppDAO.save(context, webApp);
        return webApp;
    }

    @Override
    public void delete(Context context, WebApp webApp) throws SQLException {
        webAppDAO.delete(context, webApp);
    }

    @Override
    public List<WebApp> findAll(Context context) throws SQLException {
        return webAppDAO.findAll(context, WebApp.class);
    }

    @Override
    public List<WebApp> getApps()
    {
        ArrayList<WebApp> apps = new ArrayList<>();

        Context context = null;
        HttpHead method = null;
        try {
            context = new Context();
            List<WebApp> webApps = findAll(context);

            for (WebApp app : webApps)
            {
                method = new HttpHead(app.getUrl());
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(method);
                int status = response.getStatusLine().getStatusCode();
                if (status != HttpStatus.SC_OK)
                {
                    delete(context, app

                    );
                    continue;
                }

                apps.add(app);
            }
        } catch (SQLException e) {
            log.error("Unable to list running applications", e);
        } catch (IOException e) {
            log.error("Failure checking for a running webapp", e);
        } finally {
            if (null != method)
            {
                method.releaseConnection();
            }
            if (null != context)
            {
                context.abort();
            }
        }

        return apps;
    }
}
