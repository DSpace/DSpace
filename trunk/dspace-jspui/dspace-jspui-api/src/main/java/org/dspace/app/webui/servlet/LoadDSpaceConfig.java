/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import javax.servlet.http.HttpServlet;
import java.net.URL;
import java.net.URLConnection;

/**
 * Simple servlet to load in DSpace and log4j configurations. Should always be
 * started up before other servlets (use <loadOnStartup>)
 * 
 * This class holds code to be removed in the next version of the DSpace XMLUI,
 * it is now managed by a Shared Context Listener inthe dspace-api project.
 * 
 * It is deprecated, rather than removed to maintain backward compatibility for
 * local DSpace 1.5.x customized overlays.
 * 
 * TODO: Remove in trunk
 *
 * @deprecated Use Servlet Context Listener provided in dspace-api (remove in >
 *             1.5.x)
 * @author Robert Tansley
 * @version $Revision$
 */
public class LoadDSpaceConfig extends HttpServlet
{
    private static final Logger LOG = Logger.getLogger(LoadDSpaceConfig.class);

    public void init()
    {
        // On Windows, URL caches can cause problems, particularly with undeployment
        // So, here we attempt to disable them if we detect that we are running on Windows
        try
        {
            String osName = System.getProperty("os.name");
            if (osName != null)
            {
                osName = osName.toLowerCase();
            }

            if (osName != null && osName.contains("windows"))
            {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        }
        // Any errors thrown in disabling the caches aren't significant to
        // the normal execution of the application, so we ignore them
        catch (RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage(), e);
        }

        if(!ConfigurationManager.isConfigured())
        {
            // Get config parameter
            String config = getServletContext().getInitParameter("dspace-config");

            // Load in DSpace config
            ConfigurationManager.loadConfig(config);
        }

    }
}
