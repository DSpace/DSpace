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
import java.util.Date;
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

    private final String KIND;

    protected final Date started;

    private ObjectInstance mbeanInstance;

    private AbstractDSpaceWebapp()
    {
        KIND = null;
        started = null;
    }
/**
 * Construct a particular kind of DSpace application.
 *
 * @param kind what kind of application is this?  (XMLUI, JSPUI, etc.)
 */
    public AbstractDSpaceWebapp(String kind)
    {
        KIND = kind;
        started = new Date();
    }

    /** Record that this application is running. */
    public void register()
    {
        String url = ConfigurationManager.getProperty("dspace.url");
        if (null == url)
        {
            throw new IllegalStateException("dspace.url is undefined");
        }

        // Create the database entry
        Timestamp now = new Timestamp(new Date().getTime());
        try {
            Context context = new Context();
            DatabaseManager.updateQuery(context,
                    "DELETE FROM Webapp WHERE AppName = ?", KIND);
            DatabaseManager.updateQuery(context,
                    "INSERT INTO Webapp (AppName, URL, Started) VALUES(?, ?, ?);",
                    KIND, url, now);
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
                    "DELETE FROM Webapp WHERE AppName = ?", KIND);
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

    /* DSpaceWebappMXBean methods */

    @Override
    public String getKind()
    {
        return KIND;
    }

    @Override
    public String getURL()
    {
        return ConfigurationManager.getProperty("dspace.url");
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
        return new ObjectName(DSpaceWebappMXBean.class.getName(), "Kind", KIND);
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
}
