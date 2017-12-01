/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent a DSpace application while it is running.  This helps us report
 * which applications *are* running, by exposing a record that can be viewed
 * externally.
 *
 * @author mwood
 */
abstract public class AbstractDSpaceWebapp
        implements DSpaceWebappMXBean
{
    private static final Logger log = LoggerFactory.getLogger(AbstractDSpaceWebapp.class);

    protected String kind;

    protected Date started;

    protected String url;

    private TableRow row;

    /** Prevent null instantiation. */
    protected AbstractDSpaceWebapp()
    {
    }

    /**
     * Construct a particular kind of DSpace application.
     *
     * @param kind what kind of application is this?  (XMLUI, JSPUI, etc.)
     */
    public AbstractDSpaceWebapp(String kind)
    {
        this.kind = kind;

        started = new Date();

        url = ConfigurationManager.getProperty("dspace.url");
        if (null == url)
        {
            throw new IllegalStateException("dspace.url is undefined");
        }
    }

    /** Record that this application is running. */
    public void register()
    {
        // Create the database entry
        Timestamp now = new Timestamp(started.getTime());
        try {
            Context context = new Context();
            row = DatabaseManager.create(context, "Webapp");
            row.setColumn("AppName", kind);
            row.setColumn("URL", url);
            row.setColumn("Started", now);
            row.setColumn("isUI", isUI() ? 1 : 0); // update won't widen boolean to integer
            DatabaseManager.update(context, row);
            context.complete();
        } catch (SQLException e) {
            log.error("Failed to record startup in Webapp table.", e);
        }
    }

    /** Record that this application is not running. */
    public void deregister()
    {
        // Remove the database entry
        try {
            Context context = new Context();
            DatabaseManager.delete(context, row);
            context.complete();
        } catch (SQLException e) {
            log.error("Failed to record shutdown in Webapp table.", e);
        }
    }

    /** Return the list of running applications. */
    static public List<AbstractDSpaceWebapp> getApps()
    {
        ArrayList<AbstractDSpaceWebapp> apps = new ArrayList<AbstractDSpaceWebapp>();
        TableRowIterator tri;

        Context context = null;
        HttpMethod request = null;
        try {
            context = new Context();
            tri = DatabaseManager.queryTable(context, "Webapp",
                    "SELECT * FROM Webapp");

            for (TableRow row : tri.toList())
            {
                DSpaceWebapp app = new DSpaceWebapp();
                app.kind = row.getStringColumn("AppName");
                app.url = row.getStringColumn("URL");
                app.started = row.getDateColumn("Started");
                app.uiQ = row.getBooleanColumn("isUI");

                HttpClient client = new HttpClient();
                request = new HeadMethod(app.url);
                int status = client.executeMethod(request);
                request.getResponseBody();
                if (status != HttpStatus.SC_OK)
                {
                    DatabaseManager.delete(context, row);
                    context.commit();
                    continue;
                }

                apps.add(app);
            }
        } catch (SQLException e) {
            log.error("Unable to list running applications", e);
        } catch (HttpException e) {
            log.error("Failure checking for a running webapp", e);
        } catch (IOException e) {
            log.error("Failure checking for a running webapp", e);
        } finally {
            if (null != request)
            {
                request.releaseConnection();
            }
            if (null != context)
            {
                context.abort();
            }
        }

        return apps;
    }

    /** Container for retrieved database rows. */
    static private class DSpaceWebapp
            extends AbstractDSpaceWebapp
    {
        private boolean uiQ;

        @Override
        public boolean isUI()
        {
            return uiQ;
        }
    }

    /* DSpaceWebappMXBean methods */

    @Override
    public String getKind()
    {
        return kind;
    }

    @Override
    public String getURL()
    {
        return url;
    }

    @Override
    public String getStarted()
    {
        return started.toString();
    }
}
