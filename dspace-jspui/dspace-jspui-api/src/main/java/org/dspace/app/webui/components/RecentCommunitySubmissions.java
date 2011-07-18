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
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.plugin.CommunityHomeProcessor;
import org.dspace.plugin.PluginException;

import org.dspace.app.webui.components.RecentSubmissionsManager;

/**
 * This class obtains recent submissions to the given community by
 * implementing the CommunityHomeProcessor.
 * 
 * @author Richard Jones
 *
 */
public class RecentCommunitySubmissions implements CommunityHomeProcessor
{
	/**
	 * blank constructor - does nothing
	 *
	 */
	public RecentCommunitySubmissions()
	{
		
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.plugin.CommunityHomeProcessor#process(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.content.Community)
	 */
	public void process(Context context, HttpServletRequest request, HttpServletResponse response, Community community) 
		throws PluginException, AuthorizeException
	{
		try
		{
			RecentSubmissionsManager rsm = new RecentSubmissionsManager(context);
			RecentSubmissions recent = rsm.getRecentSubmissions(community);
			request.setAttribute("recently.submitted", recent);
		}
		catch (RecentSubmissionsException e)
		{
			throw new PluginException(e);
		}
	}

	

}
