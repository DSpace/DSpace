/*
 * BrowserServlet.java
 *
 * Version: $Revision:  $
 *
 * Date: $Date:  $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for browsing through indices, as they are defined in 
 * the configuration.  This class can take a wide variety of inputs from
 * the user interface:
 * 
 * - type:	the type of browse (index name) being performed
 * - order: (ASC | DESC) the direction for result sorting
 * - value: A specific value to find items around.  For example the author name or subject
 * - month: integer specification of the month of a date browse
 * - year: integer specification of the year of a date browse
 * - starts_with: string value at which to start browsing
 * - vfocus: start browsing with a value of this string
 * - focus: integer id of the item at which to start browsing
 * - rpp: integer number of results per page to display
 * - sort_by: integer specification of the field to search on
 * - etal: integer number to limit multiple value items specified in config to
 * 
 * @author Richard Jones
 * @version $Revision:  $
 */
public class BrowserServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BrowserServlet.class);

    /**
     * Do the usual DSpace GET method.  You will notice that browse does not currently
     * respond to POST requests.
     */
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	try
    	{
    		// all browse requests currently come to GET.
    		
    		// first, lift all the stuff out of the request that we might need
    		String type = request.getParameter("type");
    		String order = request.getParameter("order");
    		String value = request.getParameter("value");
            String valueLang = request.getParameter("value_lang");
    		String month = request.getParameter("month");
            String year = request.getParameter("year");
            String startsWith = request.getParameter("starts_with");
            String valueFocus = request.getParameter("vfocus");
            String valueFocusLang = request.getParameter("vfocus_lang");
            int focus = UIUtil.getIntParameter(request, "focus");
    		int resultsperpage = UIUtil.getIntParameter(request, "rpp");
    		int sortBy = UIUtil.getIntParameter(request, "sort_by");
    		int etAl = UIUtil.getIntParameter(request, "etal");
    		
    		// get the community or collection location for the browse request
    		// Note that we are only interested in getting the "smallest" container,
    		// so if we find a collection, we don't bother looking up the community
    		Collection collection = null;
    		Community community = null;
    		collection = UIUtil.getCollectionLocation(request);
    		if (collection == null)
    		{
    			community = UIUtil.getCommunityLocation(request);
    		}
    		
    		// process the input, performing some inline validation
    		if (type == null || "".equals(type))
        	{
    			showError(context, request, response);
        	}
        	BrowseIndex bi = BrowseIndex.getBrowseIndex(type);
        	if (bi == null)
        	{
        		throw new BrowseException("There is no browse index of the type: " + type);
        	}
        	
        	// if no resultsperpage set, default to 20
        	if (resultsperpage == -1)
        	{
        		resultsperpage = 20;
        	}
        	
        	// if no order parameter, default to ascending
        	if (order == null || "".equals(order))
        	{
        		order = "ASC";
        	}
        	
        	// if year and perhaps month have been selected, we translate these into "startsWith"
        	// if startsWith has already been defined then it is overwritten
        	if (year != null && !"".equals(year) && !"-1".equals(year))
        	{
        		startsWith = year;
        		if ((month != null) && !"-1".equals(month) && !"".equals(month))
        		{
        			// subtract 1 from the month, so the match works appropriately
        			if ("ASC".equals(order))
        			{
        				month = Integer.toString((Integer.parseInt(month) - 1));
        			}
        			
        			// They've selected a month as well
        			if (month.length() == 1)
        			{
        				// Ensure double-digit month number
        				month = "0" + month;
        			}
        			
        			startsWith = year + "-" + month;
        		}
        	}
        	
        	// determine which level of the browse we are at: 0 for top, 1 for second
        	int level = 0;
        	if (value != null)
        	{
        		level = 1;
        	}
        	
        	// if a value has been specified but a sort_by parameter has not been, we
        	// need to set it as a default
        	if (value != null && sortBy == -1)
        	{
        		Map map = bi.getSortOptions();
        		if (map == null)
        		{
        			throw new BrowseException("Value browse cannot complete without a sortable column");
        		}
        		if (map.size() == 0)
        		{
        			throw new BrowseException("Value browse cannot complete without a sortable column");
        		}
        		sortBy = ((Integer) map.keySet().iterator().next()).intValue();
        	}
        	
        	// if sortBy is still not set, set it to 0, which is default to use the primary index value
        	if (sortBy == -1)
        	{
        		sortBy = 0;
        	}
        
        	// figure out the setting for author list truncation
        	if (etAl == -1)		// there is no limit, or the UI says to use the default
        	{
        		int limitLine = ConfigurationManager.getIntProperty("webui.browse.author-limit");
        		if (limitLine != 0)
        		{
        			etAl = limitLine;
        		}
        	}
        	else  // if the user has set a limit
            {
            	if (etAl == 0)  // 0 is the user setting for unlimited
            	{
            		etAl = -1;  // but -1 is the application setting for unlimited
            	}
            }
        	
        	// log the request
    		String comHandle = "n/a";
    		if (community != null)
    		{
    			comHandle = community.getHandle();
    		}
    		String colHandle = "n/a";
    		if (collection != null)
    		{
    			colHandle = collection.getHandle();
    		}
        	
        	String arguments = "type=" + type + ",order=" + order + ",value=" + value + 
				",month=" + month + ",year=" + year + ",starts_with=" + startsWith +
				",vfocus=" + valueFocus + ",focus=" + focus + ",rpp=" + resultsperpage +
				",sort_by=" + sortBy + ",community=" + comHandle + ",collection=" + colHandle +
				",level=" + level + ",etal=" + etAl;

        	log.info(LogManager.getHeader(context, "browse", arguments));
        	
        	// set up a BrowseScope and start loading the values into it
        	BrowserScope scope = new BrowserScope(context);
        	scope.setBrowseIndex(bi);
        	scope.setOrder(order);
        	scope.setValue(value);
            scope.setValueLang(valueLang);
        	scope.setFocus(focus);
        	scope.setValueFocus(valueFocus);
            scope.setValueFocusLang(valueFocusLang);
        	scope.setStartsWith(startsWith);
        	scope.setResultsPerPage(resultsperpage);
        	scope.setSortBy(sortBy);
        	scope.setBrowseLevel(level);
        	
        	// assign the scope of either Community or Collection if necessary
        	if (community != null)
        	{
        		scope.setBrowseContainer(community);
        	}
        	else if (collection != null)
        	{
        		scope.setBrowseContainer(collection);
        	}
        	
        	// now start up a browse engine and get it to do the work for us
        	BrowseEngine be = new BrowseEngine(context);
        	BrowseInfo binfo = be.browse(scope);
        	
        	// add the etAl limit to the BrowseInfo object
        	binfo.setEtAl(etAl);
        	
        	request.setAttribute("browse.info", binfo);
        	
        	if (binfo.hasResults())
        	{
        		if (bi.isSingle() && !scope.isSecondLevel())
        		{
        			showSinglePage(context, request, response);
        		}
        		else
        		{
        			showFullPage(context, request, response);
        		}
        	}
        	else
        	{
        		showNoResultsPage(context, request, response);
        	}
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }
    
    /**
     * Display the error page
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showError(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	JSPManager.showJSP(request, response, "/browse/error.jsp");
    }
    
    /**
     * Display the No Results page
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showNoResultsPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	
    	JSPManager.showJSP(request, response, "/browse/no-results.jsp");
    }
    
    /**
     * Display the single page.  This is the page which lists just the single values of a 
     * metadata browse, not individual items.  Single values are links through to all the items
     * that match that metadata value
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showSinglePage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	
    	JSPManager.showJSP(request, response, "/browse/single.jsp");
    }
    
    /**
     * Display a full item listing.
     * 
     * @param context
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void showFullPage(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	
    	JSPManager.showJSP(request, response, "/browse/full.jsp");
    }
}
