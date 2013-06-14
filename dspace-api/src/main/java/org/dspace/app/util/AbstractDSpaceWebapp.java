/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
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
        implements DSpaceWebappMXBean, MBeanRegistration
{
    private static final Logger log = LoggerFactory.getLogger(AbstractDSpaceWebapp.class);

    protected String kind;

    protected Date started;

    protected String url;

    private ObjectInstance mbeanInstance;

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
        Timestamp now = new Timestamp(new Date().getTime());
        try {
            Context context = new Context();
            DatabaseManager.updateQuery(context,
                    "DELETE FROM Webapp WHERE AppName = ?", kind);
            DatabaseManager.updateQuery(context,
                    "INSERT INTO Webapp (AppName, URL, Started, isUI) VALUES(?, ?, ?, ?);",
                    kind, url, now, isUI() ? 1 : 0);
            context.complete();
        } catch (SQLException e) {
            log.error("Failed to record startup in Webapp table.", e);
        }

        // Create the MBean
        try
        {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbeanInstance = mbs.registerMBean(this, null);
        } catch (InstanceAlreadyExistsException ex) {
            log.error("Can't register webapp MBean:  {}", ex.getMessage());
        } catch (MBeanRegistrationException ex) {
            log.error("Can't register webapp MBean:  {}", ex.getMessage());
        } catch (NotCompliantMBeanException ex) {
            log.error("Can't register webapp MBean:  {}", ex.getMessage());
        }
    }

    /** Record that this application is not running. */
    public void deregister()
    {
        // Remove the database entry
        try {
            Context context = new Context();
            DatabaseManager.updateQuery(context,
                    "DELETE FROM Webapp WHERE AppName = ?", kind);
            context.complete();
        } catch (SQLException e) {
            log.error("Failed to record shutdown in Webapp table.", e);
        }

        // Tear down the WebappMBean
        if (null != mbeanInstance)
        {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.unregisterMBean(mbeanInstance.getObjectName());
            } catch (InstanceNotFoundException ex) {
                log.error("Can't unregister webapp MBean:  {}", ex.getMessage());
            } catch (MBeanRegistrationException ex) {
                log.error("Can't unregister webapp MBean:  {}", ex.getMessage());
            }
        }
    }

    /** Return the list of running applications. */
    static public List<AbstractDSpaceWebapp> getApps()
    {
        ArrayList<AbstractDSpaceWebapp> apps = new ArrayList<AbstractDSpaceWebapp>();
        TableRowIterator tri;

        Context context = null;
        try {
            context = new Context();
            tri = DatabaseManager.queryTable(context, "Webapp",
                    "SELECT AppName, URL, Started, isUI FROM Webapp");

            for (TableRow row : tri.toList())
            {
                DSpaceWebapp app = new DSpaceWebapp();
                app.kind = row.getStringColumn("AppName");
                app.url = row.getStringColumn("URL");
                app.started = row.getDateColumn("Started");
                app.uiQ = row.getBooleanColumn("isUI");
                apps.add(app);
            }
        } catch (SQLException e) {
            log.error("Unable to list running applications", e);
        } finally {
            if (null != context)
            {
                context.abort();
            }
        }

        return apps;
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

    /* MBeanRegistration methods */

    @Override
    public ObjectName preRegister(MBeanServer mbs, ObjectName on)
            throws Exception
    {
        return new ObjectName(DSpaceWebappMXBean.class.getName(), "Kind", kind);
    }

    @Override
    public void postRegister(Boolean bln)
    {
    }

    @Override
    public void preDeregister()
            throws Exception
    {
    }

    @Override
    public void postDeregister()
    {
    }

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
}
