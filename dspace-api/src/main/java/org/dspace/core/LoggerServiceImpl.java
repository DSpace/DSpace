/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.dspace.services.ConfigurationService;
import org.dspace.services.KernelStartupCallbackService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Service which simply initializes DSpace logging *after* the kernel starts
 *
 * @author Tim Donohue
 */
public class LoggerServiceImpl implements KernelStartupCallbackService
{
    /** log4j category */
    private static Logger log = Logger.getLogger(LoggerServiceImpl.class);

    // System property which will disable DSpace's log4j setup
    private final String LOG_DISABLE_PROPERTY = "dspace.log.init.disable";

    // Logging settings which are specified in DSpace's configuration
    private final String LOG_CONFIG_PROPERTY = "log.init.config";

    /**
     * After kernel starts up, initialize Log4j based on the logging settings
     * in our ConfigurationService.
     */
    @Override
    public void executeCallback()
    {
        try
        {
            /*
             * Initialize Logging once ConfigurationManager is initialized.
             *
             * This is controlled by a property in dspace.cfg.  If the property
             * is absent then nothing will be configured and the application
             * will use the defaults provided by log4j.
             *
             * Property format is:
             *
             * log.init.config = ${dspace.dir}/config/log4j.properties
             * or
             * log.init.config = ${dspace.dir}/config/log4j.xml
             *
             * See default log4j initialization documentation here:
             * http://logging.apache.org/log4j/docs/manual.html
             *
             * If there is a problem with the file referred to in
             * "log.configuration", it needs to be sent to System.err
             * so do not instantiate another Logging configuration.
             *
             */
            ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
            String dsLogConfiguration = config.getProperty(LOG_CONFIG_PROPERTY);

            if (dsLogConfiguration == null || System.getProperty(LOG_DISABLE_PROPERTY) != null)
            {
                /*
                 * Do nothing if log config not set in dspace.cfg or "dspace.log.init.disable"
                 * system property set. Leave it upto log4j to properly init its logging
                 * via classpath or system properties.
                 */
                info("Using default log4j provided log configuration." +
                        "  If unintended, check your dspace.cfg for (" +LOG_CONFIG_PROPERTY+ ")");
            }
            else
            {
                info("Using dspace provided log configuration (" +LOG_CONFIG_PROPERTY+ ")");

                File logConfigFile = new File(dsLogConfiguration);

                if(logConfigFile.exists())
                {
                    info("Loading: " + dsLogConfiguration);

                    // Check if we have an XML config
                    if(logConfigFile.getName().endsWith(".xml"))
                    {
                        // Configure log4j via the DOMConfigurator
                        DOMConfigurator.configure(logConfigFile.toURI().toURL());
                    }
                    else // Otherwise, assume a Properties file
                    {
                        // Parse our log4j properties file
                        Properties log4jProps = new Properties();
                        try(InputStream fis = new FileInputStream(logConfigFile))
                        {
                            log4jProps.load(fis);
                        }
                        catch(IOException e)
                        {
                            fatal("Can't load dspace provided log4j configuration from " + logConfigFile.getAbsolutePath(), e);
                        }

                        // Configure log4j based on all its properties
                        PropertyConfigurator.configure(log4jProps);
                    }
                }
                else
                {
                    info("File does not exist: " + dsLogConfiguration);
                }
            }

        }
        catch (MalformedURLException e)
        {
            fatal("Can't load dspace provided log4j configuration", e);
            throw new IllegalStateException("Cannot load dspace provided log4j configuration",e);
        }
    }

    /**
     * Attempt to log an INFO statement. If Log4j is not yet setup, send to System OUT
     * @param string
     */
    private void info(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("INFO: " + string);
        }
        else
        {
            log.info(string);
        }
    }

    /**
     * Attempt to log a WARN statement. If Log4j is not yet setup, send to System OUT
     * @param string
     */
    private void warn(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("WARN: " + string);
        }
        else
        {
            log.warn(string);
        }
    }

    /**
     * Attempt to log a FATAL statement. If Log4j is not yet setup, send to System ERR
     * @param string
     * @param e
     */
    private void fatal(String string, Exception e)
    {
        if (!isLog4jConfigured())
        {
            System.err.println("FATAL: " + string);
            e.printStackTrace(System.err);
        }
        else
        {
            log.fatal(string, e);
        }
    }

    /**
     * Only current solution available to detect if log4j is truly configured.
     * <p>
     * Based on samples here: http://wiki.apache.org/logging-log4j/UsefulCode
     */
    private boolean isLog4jConfigured()
    {
        Enumeration<?> appenders = org.apache.log4j.LogManager.getRootLogger()
                .getAllAppenders();

        if (!(appenders instanceof org.apache.log4j.helpers.NullEnumeration))
        {
            return true;
        }
        else
        {
            Enumeration<?> loggers = org.apache.log4j.LogManager.getCurrentLoggers();
            while (loggers.hasMoreElements())
            {
                Logger c = (Logger) loggers.nextElement();
                if (!(c.getAllAppenders() instanceof org.apache.log4j.helpers.NullEnumeration))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
