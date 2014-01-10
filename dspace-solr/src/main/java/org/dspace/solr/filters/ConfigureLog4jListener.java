/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.solr.filters;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Initialize Log4J at application startup.
 * This class mimics the default Log4J initialization procedure, except
 * that it is controlled by context parameters rather than system properties.
 *
 * @author Mark H. Wood
 */
public class ConfigureLog4jListener
        implements ServletContextListener
{
    public void contextInitialized(ServletContextEvent sce)
    {
        ServletContext ctx = sce.getServletContext();

        String logConfig = ctx.getInitParameter("log4j.configuration");
        if (null == logConfig)
            logConfig = "log4j.properties";

        URL configURL;
        try {
            configURL = new File(logConfig).toURI().toURL();
        } catch (MalformedURLException e) {
            configURL = Loader.getResource(logConfig);
        }

        if (null == configURL)
        {
            ctx.log("Log4J configuration not found.  Left unconfigured.");
            return;
        }
        else
        {
            ctx.log(" In context " + ctx.getContextPath() +
                    ", configuring Log4J from " + configURL.toExternalForm());

            String configuratorName = ctx.getInitParameter("log4j.configuratorClass");
            if (null != configuratorName)
            {
                Configurator configurator;
                try
                {
                    configurator = (Configurator) Class.forName(configuratorName).newInstance();
                } catch (Exception ex)
                {
                    ctx.log("Unable to load custom Log4J configuration class '"
                            + configuratorName + "':  " + ex.getMessage());
                    return;
                }

                configurator.doConfigure(configURL, new Hierarchy(new RootLogger(Level.OFF)));
            }
            else if (configURL.getFile().endsWith(".xml"))
                DOMConfigurator.configure(configURL);
            else
                PropertyConfigurator.configure(configURL);
        }
    }

    public void contextDestroyed(ServletContextEvent sce)
    {
        // Nothing to be done
    }
}
