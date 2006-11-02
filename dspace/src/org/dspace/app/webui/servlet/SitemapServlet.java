/*
 * SitemapServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Servlet for retrieving sitemaps.
 * 
 * The servlet can serve-up sitemaps in the following formats:
 * <code>/sitemp?html=</code>
 * <code>/sitemap?google=</code>
 * 
 * google=0 for the index map.
 * google=1 (2.3...4 etc) for each sitemap.
 * 
 * html=0 (1.2...3 etc) for each sitemap
 * 
 * @author Stuart Lewis
 * @version $Revision$
 */
public class SitemapServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SitemapServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	// Which sitemap has been requested?
    	String html = request.getParameter("html");
    	String google = request.getParameter("google");
    	
    	if ((html == null) && (google == null))
    	{
    		// Show the 404
    		JSPManager.showJSP(request, response, "/error/404.jsp");
    		return;
    	}
    	else
    	{
    		// If multiple maps have been chosen, return html in preference
    		String map = html;
    		String type = html;;
    		if (google != null)
    		{
    			map = google;
    			type = "google";
    		}
    		
    		// Which map do we need to display? (Can only be numeric)
    		int whichMap;
    	    try
    		{
    	    	whichMap = Integer.parseInt(map);
    		}
    	    catch (NumberFormatException nfe)
    	    {
    	    	JSPManager.showJSP(request, response, "/error/404.jsp");
    	    	return;
    	    }
    	    
    	    // Call the appropriate metho to return the requested map
    	    if (type.equals("google"))
    	    {
    	    	this.google(context, request, response, whichMap);
    	    }
    	    else
    	    {
    	    	this.html(context, request, response, whichMap);
        	}
    	}
    }
    
    /**
     * A method to return the requested html sitemap.
     * 
     * @param context The DSpace Context
     * @param request The servlet request object
     * @param response The servlet response object
     * @param whichMap The number of the sitemap requested
     * 
     * @throws ServletException
     * @throws IOException
     */
    private void html(Context context, HttpServletRequest request,
                        HttpServletResponse response, int whichMap) 
                                                 throws ServletException, IOException
    {
    	// Is this a valid sitemap? Lets see...
	    try
	    {
	    	String home = ConfigurationManager.getProperty("dspace.dir") + "/sitemaps/";
	    	File f;
	    	if (whichMap == 0)
    		{
	    		f = new File(home, "html-sitemap_index.html");
    		}
	    	else
	        {
	    		f = new File(home, "html-sitemap" + whichMap + ".html");
	        }
	        
			if ((f.isFile()) && (f.canRead()))
			{
				// Set the appropriate header
				response.setHeader("Content-Type", "text/html");
				
				// Set the response size
				response.setHeader("Content-Length", "" + f.length());

				// Pipe out the file
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(f)); 
				BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
				byte[]buffer = new byte[1024]; 
				int size = in.read(buffer); 
				while (size != -1)
				{ 
					output.write( buffer, 0, size ); 
					size = in.read(buffer); 
				}
				in.close();
				output.flush(); 
				log.info(LogManager.getHeader(context, "sitemap download",
                         "HTML sitemap = " + whichMap));
			}
			else
			{
				// The sitemaps requested doesn't exist
				JSPManager.showJSP(request, response, "/error/404.jsp");
		    	return;
			}
	    }
	    catch (Exception e)
	    {
	    	// Something went wrong :(
	    	JSPManager.showJSP(request, response, "/error/404.jsp");
	    	return;
	    }
	}
    
    /**
     * A method to return the requested google sitemap.
     * 
     * @param context The DSpace Context
     * @param request The servlet request object
     * @param response The servlet response object
     * @param whichMap The number of the sitemap requested
     * 
     * @throws ServletException
     * @throws IOException
     */
    private void google(Context context, HttpServletRequest request,
                        HttpServletResponse response, int whichMap) 
                                                 throws ServletException, IOException
    {
    	// Is this a valid sitemap? Lets see...
	    try
	    {
	    	String home = ConfigurationManager.getProperty("dspace.dir") + "/sitemaps/";
	    	File f;
	    	if (whichMap == 0)
    		{
	    		f = new File(home, "google-sitemap_index.xml");
    		}
	    	else
	        {
	    		f = new File(home, "google-sitemap" + whichMap + ".xml.gz");
	        }
	    	
			if ((f.isFile()) && (f.canRead()))
			{
				// Set the appropriate header
				if (f.getName().endsWith(".gz"))
				{
					response.setHeader("Content-Type", "application/x-gzip");
				}
				else
				{
					response.setHeader("Content-Type", "application/xml");
				}
				
				// Set the response size
				response.setHeader("Content-Length", "" + f.length());

				// Pipe out the file
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(f)); 
				BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
				byte[]buffer = new byte[1024]; 
				int size = in.read(buffer); 
				while (size != -1)
				{ 
					output.write( buffer, 0, size ); 
					size = in.read(buffer); 
				}
				in.close();
				output.flush(); 
				log.info(LogManager.getHeader(context, "sitemap download",
                         "Google sitemap = " + whichMap));
			}
			else
			{
				// The sitemaps requested doesn't exist
				JSPManager.showJSP(request, response, "/error/404.jsp");
		    	return;
			}
	    }
	    catch (Exception e)
	    {
	    	// Something went wrong :(
	    	JSPManager.showJSP(request, response, "/error/404.jsp");
	    	return;
	    }
	}
}
