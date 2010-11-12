/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import javax.servlet.http.HttpServlet;

import org.dspace.core.ConfigurationManager;

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
 *             
 * @author Robert Tansley
 */
public class LoadDSpaceConfig extends HttpServlet
{	
    public void init()
    {
        if(!ConfigurationManager.isConfigured())
        {
            // Get config parameter
            String config = getServletContext().getInitParameter("dspace-config");

            // Load in DSpace config
            ConfigurationManager.loadConfig(config);
        }
    }
}
