/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

/**
 * This class add top communities object to the request attributes to use in
 * the site home page implementing the SiteHomeProcessor.
 * 
 * @author Andrea Bollini
 * 
 */
public class TopCommunitiesSiteProcessor implements SiteHomeProcessor
{

	private CommunityService communityService;
	
    /**
     * blank constructor - does nothing.
     * 
     */
    public TopCommunitiesSiteProcessor()
    {
    	 communityService = ContentServiceFactory.getInstance().getCommunityService();
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response) throws PluginException,
            AuthorizeException
    {
        // Get the top communities to shows in the community list
        List<Community> communities;
        try
        {
            communities = communityService.findAllTop(context);
        }
        catch (SQLException e)
        {
            throw new PluginException(e.getMessage(), e);
        }
        request.setAttribute("communities", communities);
    }

}
