/*
 * AbstractBrowse.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/04/25 15:24:23 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.browse.Browse;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * This is an abstract search page that may be specialized to the various forms:
 * by titles,  dates, or authors.
 * 
 * FIXME: Needs Support browse by subject.
 * 
 * @author Scott Phillips
 * @author Paulo Jobim
 */
public class AbstractBrowse extends AbstractDSpaceTransformer
{

    /** How many results should appear on one page? */
    protected static final int RESULTS_PER_PAGE = 11;
    

    /** The possible browsing modes */
    protected static final int MODE_BY_TITLE  = 1;
    protected static final int MODE_BY_DATE   = 2;
    protected static final int MODE_BY_AUTHOR = 3;
    protected static final int MODE_BY_AUTHOR_ITEM = 4;
    protected static final int MODE_BY_SUBJECT = 5;
    protected static final int MODE_BY_SUBJECT_ITEM = 6;
    
    /**
     * Browsing related parameters, the preformBrowse() method will 
     * determine these cached results. 
     */
    protected BrowseInfo browseInfo;
    protected BrowseScope browseScope;
    protected boolean browseItem; 
    
    /**
     * Preform the browse for the given mode.
     * 
     * @param mode
     *          The browsing mode.
     */
    public void performBrowse(int mode) throws SQLException, UIException 
    {
    	// Check to see if we have the browse results cached.
    	if (this.browseInfo != null && this.browseScope != null)
    		return;	
    	
        // We will resolve the HTTP request parameters into a scope
        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);

        // HTTP Parameters
        String top = request.getParameter("top");
        String bottom = request.getParameter("bottom");
        String startsWith = request.getParameter("startsWith");
        String month = request.getParameter("month");
        String year = request.getParameter("year");
        String author = request.getParameter("author");
        String subject = request.getParameter("subject");

        try {
	        if (top != null)
	            top = URLDecoder.decode(top,Constants.DEFAULT_ENCODING);
	        if (bottom != null)
	            bottom = URLDecoder.decode(bottom,Constants.DEFAULT_ENCODING);
	        if (author !=  null)
	            author = URLDecoder.decode(author,Constants.DEFAULT_ENCODING);
	        if (subject !=  null)
	        	subject = URLDecoder.decode(subject,Constants.DEFAULT_ENCODING);
        } 
        catch (UnsupportedEncodingException uee) 
        {
        	throw new UIException("Unable to decode url parameters: top, bottom, subject or author.",uee);
        }
        
        // Buld the browse scope
        BrowseScope scope = new BrowseScope(context);
        
        scope.setTotal(RESULTS_PER_PAGE);

        if (top != null && !"".equals(top))
        {
            if (mode == MODE_BY_AUTHOR || mode == MODE_BY_SUBJECT)
            {   
                scope.setFocus(top);
                scope.setNumberBefore(0);
            }
            else 
            {
                Item item = (Item) HandleManager.resolveToObject(context, top);
                scope.setFocus(item);
                scope.setNumberBefore(0);
            }

        }
        else if (bottom != null && !"".equals(bottom))
        {
            
            if (mode == MODE_BY_AUTHOR || mode == MODE_BY_SUBJECT)
            {
                scope.setFocus(bottom);
                scope.setNumberBefore(RESULTS_PER_PAGE - 1);
            }
            else
            {
                Item item = (Item) HandleManager.resolveToObject(context, bottom);
                scope.setFocus(item);
                scope.setNumberBefore(RESULTS_PER_PAGE - 1);
            }
        }
        else if (startsWith != null && !"".equals(startsWith))
        {
            scope.setFocus(startsWith);
            scope.setNumberBefore(0);
        }
        else if (mode == MODE_BY_DATE && year != null
                && !"".equals(year))
        {
            if (month != null & !"-1".equals(month))
            {
                if (month.length() == 1)
                    month = "0" + month;
                scope.setFocus(year + "-" + month);
                scope.setNumberBefore(0);
            }
            else
            {
                scope.setFocus(year + "-01");
                scope.setNumberBefore(0);
            }
        }
        else if (mode == MODE_BY_AUTHOR_ITEM)
        {
            scope.setFocus(author);
        }
        else if (mode == MODE_BY_SUBJECT_ITEM)
        {
            scope.setFocus(subject);
        }

        // Are we in a community or collection?
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso instanceof Community)
            scope.setScope((Community) dso);
        if (dso instanceof Collection)
            scope.setScope((Collection) dso);

        // Query the browse index
        BrowseInfo browseInfo;

        if (mode == MODE_BY_TITLE)
        {
            browseInfo = Browse.getItemsByTitle(scope);
        }
        else if (mode == MODE_BY_DATE)
        {
            browseInfo = Browse.getItemsByDate(scope, false);
        }
        else if (mode == MODE_BY_AUTHOR)
        {
        	browseInfo = Browse.getAuthors(scope);
        }
        else if (mode == MODE_BY_AUTHOR_ITEM)
        {
        	browseInfo = Browse.getItemsByAuthor(scope, true);
        }
        else if (mode == MODE_BY_SUBJECT)
        {
        	browseInfo = Browse.getSubjects(scope);
        }
        else if (mode == MODE_BY_SUBJECT_ITEM)
        {
        	browseInfo = Browse.getItemsBySubject(scope, true);
        }
        else
        {
            throw new UIException("Unknown mode selected.");
        }

        // Save the search results for later access.
        this.browseInfo = browseInfo;
        this.browseScope = scope;
        if (mode == MODE_BY_AUTHOR || mode == MODE_BY_SUBJECT)
        	this.browseItem = false;
        else
        	this.browseItem = true;
        
    }

    
    /**
     * The URL query string of of the previous page.
     * 
     * Note: the query string does not start with a "?" or "&" those need to be
     * added as approprate by the calle.
     * 
     * @param objectModel
     *            Cocoon' object model.
     * @return The query string if available, otherwise null.
     */
    public String previousPageURL(String baseURL)
            throws UIException
    {
        try
        {
        	// A search must be preformed first.
        	if (browseInfo == null)
        		return null;
        	
        	
            if (browseInfo.isFirst())
                return null;

            
            Map<String,String> parameters = new HashMap<String,String>();
            if (browseItem)
            {
                Item[] items = browseInfo.getItemResults();

                if (items == null || items.length <= 0)
                    return null;

                String bottom = URLEncoder.encode(items[0].getHandle(),
                        Constants.DEFAULT_ENCODING);
                
                parameters.put("bottom",bottom);
            }
            else
            {
                String[] strings = browseInfo.getStringResults();

                if (strings == null || strings.length <= 0)
                	return null;
                
                String bottom = URLEncoder.encode(strings[0],
                        Constants.DEFAULT_ENCODING);
                
                parameters.put("bottom",bottom);
            }

            return super.generateURL(baseURL,parameters);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
    }

    /**
     * The URL query string of of the next page.
     * 
     * Note: the query string does not start with a "?" or "&" those need to be
     * added as approprate by the calle.
     * 
     * @param objectModel
     *            Cocoon' object model.
     * @return The query string if available, otherwise null.
     */
    public String nextPageURL(String baseURL) throws UIException
    {
        try
        {
        	// A search must be preformed first.
        	if (browseInfo == null)
        		return null;

            if (browseInfo.isLast())
                return null;

            Map<String,String> parameters = new HashMap<String,String>();
            if (browseItem)
            {
                Item[] items = browseInfo.getItemResults();

                if (items == null || items.length <= 0)
                    return null;

                String top = URLEncoder.encode(items[items.length - 1].getHandle(),
                        Constants.DEFAULT_ENCODING);
                
                parameters.put("top",top);
            }
            else
            {
                String[] strings = browseInfo.getStringResults();
                
                if (strings == null || strings.length <= 0)
                	return null;
                
                String top = URLEncoder.encode(strings[strings.length - 1],
                        Constants.DEFAULT_ENCODING);
                
                parameters.put("top",top);
            }

            return super.generateURL(baseURL,parameters);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }
    }
    
    /**
     * Recyle
     */
    public void recycle() {
        this.browseInfo = null;
        this.browseScope = null;
    	super.recycle();
    }
}
