/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.plugin.PluginException;

/**
 * This class obtains recent submissions to the site by
 * implementing the SiteHomeProcessor.
 * 
 * @author Keiji Suzuki 
 *
 */
public class RecentSiteSubmissions implements SiteHomeProcessor
{
    /**
     * blank constructor - does nothing.
     *
     */
    public RecentSiteSubmissions()
    {
        
    }
    
    /* (non-Javadoc)
     * @see org.dspace.plugin.CommunityHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.content.Community)
     */
    @Override
    public void process(Context context, HttpServletRequest request, HttpServletResponse response) 
        throws PluginException, AuthorizeException
    {
        try
        {
            RecentSubmissionsManager rsm = new RecentSubmissionsManager(context);
            RecentSubmissions recent = rsm.getRecentSubmissions(null);
            request.setAttribute("recent.submissions", recent);

        }
        catch (RecentSubmissionsException e)
        {
            throw new PluginException(e);
        }
    }
}
