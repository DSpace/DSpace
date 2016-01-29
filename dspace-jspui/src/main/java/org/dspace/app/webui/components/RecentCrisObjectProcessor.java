/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryViewAndHighlightConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;
import org.dspace.plugin.SiteHomeProcessor;
import org.dspace.plugin.PluginException;

/**
 * This class obtains recent submissions to the site by
 * implementing the SiteHomeProcessor.
 * 
 * @author Keiji Suzuki 
 *
 */
public class RecentCrisObjectProcessor implements SiteHomeProcessor
{
    /**
     * blank constructor - does nothing.
     *
     */
    public RecentCrisObjectProcessor()
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
            String indexName = request.getParameter("recent.crisobject.processor.type");
            if(StringUtils.isNotBlank(indexName)) {            
                rsm.setIndexName(indexName);
            }
            RecentSubmissions recent = rsm.getRecentSubmissions(null);
            
            String discoveryConfiguration = request.getParameter("recent.crisobject.processor.configuration");
            if(StringUtils.isBlank(discoveryConfiguration)) {            
                discoveryConfiguration = "crisRPConfiguration";
            }
            recent.setConfiguration(SearchUtils.getRecentSubmissionConfiguration(discoveryConfiguration).getMetadataFields());
            
            request.setAttribute("recent.submissions", recent);

        }
        catch (RecentSubmissionsException e)
        {
            throw new PluginException(e);
        }
    }
}
