/*
 * LayoutTag.java
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
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.webui.servlet.FeedServlet;

/**
 * Tag for HTML page layout ("skin").
 * <P>
 * This tag <em>sets</em> request attributes that should be used by the header
 * and footer to render the page appropriately:
 * <P>
 * <ul>
 * <li><code>dspace.layout.title</code> - title of page</li>
 * <li><code>dspace.layout.locbar</code> - value will Boolean true or false
 * </li>
 * <li><code>dspace.layout.parenttitles</code> - a <code>List</code> of
 * <code>String</code>s corresponding with titles to put in the location bar.
 * Only set if <code>dspace.layout.locbar</code> is true</li>
 * <li><code>dspace.layout.parentlinks</code> - a <code>List</code> of
 * <code>String</code>s corresponding with links to put in the location bar.
 * Empty strings mean no link. Will only be set if
 * <code>dspace.layout.locbar</code> is true.</li>
 * <li><code>dspace.layout.navbar</code> - value will be "off", or the
 * navigation bar to include, e.g. "/layout/navbar_default.jsp"</li>
 * <li><code>dspace.layout.sidebar</code> - contents of the sidebar</li>
 * <li><code>dspace.current.user</code> - the EPerson currently logged in, or
 * <code>null</code> if anonymous access</li>
 * <li><code>dspace.layout.feeddata</code> - <code>String</code>. "NONE"
 * means no feed from this page; otherwise the Handle of object (community or
 * collection) the feed is from</li>
 * <li><code>dspace.layout.linkparts</code> - <code>String[]</code>. A
 * cycling sequence of: 2nd part of MIME type (e.g. <code>rdf+rss</code>);
 * title of feed; path component to go after /feed/ for the actual feed URL
 * (e.g. <code>rss_1.0</code>). Hence, this array will have 3<i>n</i>
 * elements where <i>n</i> is the number of feeds.</li>
 * </ul>
 * <p>
 *
 * * Additionally the following parameter may be set elsewhere in a Servlet.
 * <p>
 * <ul>
 * <li><code>dspace.layout.head</code> - extra data to include verbatim in
 * the &lt;head&gt; element of the page</li>
 * </ul>
 * 
 * Furthermore it sets the content type of the response to text/html using UTF-8
 * to ensure this will be returned in the HTTP header.
 * </p>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class LayoutTag extends TagSupport
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(LayoutTag.class);

    /** layout style name */
    private String style;

    /** title */
    private String title;

    /** title key (from message dictionary) */
    private String titleKey;

    /** Navigation bar type, null means none */
    private String navbar;

    /** Location bar type */
    private String locbar;

    /** Name of "parent" page */
    private String parentTitle;

    /** Name of "parent" page key (from message dictionary) */
    private String parentTitleKey;

    /** Link to "parent" page */
    private String parentLink;

    /** Contents of side bar */
    private String sidebar;

    /** Whether to add headers to prevent browsers caching the page */
    private String noCache;
    
    /** Syndication feed "autodiscovery" link data */
    private String feedData;

    public LayoutTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        ServletRequest request = pageContext.getRequest();

        // header file
        String header = "/layout/header-default.jsp";

        // Choose default style unless one is specified
        if (style != null)
        {
            header = "/layout/header-" + style.toLowerCase() + ".jsp";
        }

        // Sort out location bar
        if (locbar == null)
        {
            locbar = "auto";
        }

        // These lists will contain titles and links to put in the location
        // bar
        List parents = new ArrayList();
        List parentLinks = new ArrayList();

        if (locbar.equalsIgnoreCase("off"))
        {
            // No location bar
            request.setAttribute("dspace.layout.locbar", new Boolean(false));
        }
        else
        {
            // We'll always add "DSpace Home" to the a location bar
            parents.add(ConfigurationManager.getProperty("dspace.name"));

            if (locbar.equalsIgnoreCase("nolink"))
            {
                parentLinks.add("");
            }
            else
            {
                parentLinks.add("/");
            }

            // Add other relevant components to the location bar
            if (locbar.equalsIgnoreCase("link"))
            {
                // "link" mode - next thing in location bar is taken from
                // parameters of tag, with a link
                if (parentTitle != null)
                {
                    parents.add(parentTitle);
                    parentLinks.add(parentLink);
                }
                else if (parentTitleKey != null)
                {
                    parents.add(LocaleSupport.getLocalizedMessage(pageContext,
                            parentTitleKey));
                    parentLinks.add(parentLink);
                }

            }
            else if (locbar.equalsIgnoreCase("commLink"))
            {
                // "commLink" mode - show all parent communities
                Community[] comms = (Community[]) request
                        .getAttribute("dspace.communities");

                if (comms != null)
                {
                    for (int i = 0; i < comms.length; i++)
                    {
                        parents.add(comms[i].getMetadata("name"));
                        parentLinks.add("/handle/" + comms[i].getHandle());
                    }
                }
            }
            else if (locbar.equalsIgnoreCase("nolink"))
            {
                // "nolink" mode - next thing in location bar is taken from
                // parameters of tag, with no link
                if (parentTitle != null)
                {
                    parents.add(parentTitle);
                    parentLinks.add("");
                }
            }
            else
            {
                // Grab parents from the URL - these should have been picked up
                // by the HandleServlet
                Collection col = (Collection) request
                        .getAttribute("dspace.collection");
                Community[] comms = (Community[]) request
                        .getAttribute("dspace.communities");

                if (comms != null)
                {
                    for (int i = 0; i < comms.length; i++)
                    {
                        parents.add(comms[i].getMetadata("name"));
                        parentLinks.add("/handle/" + comms[i].getHandle());
                    }

                    if (col != null)
                    {
                        parents.add(col.getMetadata("name"));
                        parentLinks.add("/handle/" + col.getHandle());
                    }
                }
            }

            request.setAttribute("dspace.layout.locbar", new Boolean(true));
        }

        request.setAttribute("dspace.layout.parenttitles", parents);
        request.setAttribute("dspace.layout.parentlinks", parentLinks);

        // Navigation bar: "default" is default :)
        if (navbar == null)
        {
            navbar = "default";
        }

        if (navbar.equals("off"))
        {
            request.setAttribute("dspace.layout.navbar", "off");
        }
        else
        {
            request.setAttribute("dspace.layout.navbar", "/layout/navbar-"
                    + navbar + ".jsp");
        }

        // Set title
        if (title != null)
        {
            request.setAttribute("dspace.layout.title", title);
        }
        else if (titleKey != null)
        {
            request.setAttribute("dspace.layout.title", LocaleSupport
                    .getLocalizedMessage(pageContext, titleKey));
        }
        else
        {
            request.setAttribute("dspace.layout.title", "NO TITLE");
        }
        
        // Set feedData if present
        if (feedData != null && ! "NONE".equals(feedData))
        {
            // set the links' reference - community or collection
        	boolean commLinks = feedData.startsWith("comm:");
        	boolean collLinks = feedData.startsWith("coll:");
        	if ( commLinks )
        	{
                Community com = (Community)request.getAttribute("dspace.community");
        		request.setAttribute("dspace.layout.feedref", com.getHandle());
        	}
        	else if( collLinks )
        	{
        		Collection col = (Collection)request.getAttribute("dspace.collection");
        		request.setAttribute("dspace.layout.feedref", col.getHandle());
        	}
        	else //feed is across all of DSpace and not Community/Collection specific
        	{
        		request.setAttribute("dspace.layout.feedref", FeedServlet.SITE_FEED_KEY);
        	}
        	
        	// build a list of link attributes for each link format
        	String[] formats = feedData.substring(feedData.indexOf(":")+1).split(",");
        	List linkParts = new ArrayList();
        	// each link has a mime-type, title, and format (used in href URL)
        	for (int i = 0; i < formats.length; i++)
        	{
        		if("rss_1.0".equals(formats[i]))
        		{
        			linkParts.add("rdf+xml");
        		}
        		else
        		{
        			linkParts.add("rss+xml");
        		}
        		
        		if (commLinks)
        		{
        			linkParts.add("Items in Community");
        		}
        		else if(collLinks)
        		{
        			linkParts.add("Items in Collection");
        		}
        		else
        		{
        			linkParts.add("Items in " + ConfigurationManager.getProperty("dspace.name"));
        		}
        		
        		linkParts.add(formats[i]);
        	}
        	request.setAttribute("dspace.layout.linkparts", linkParts);
        }
        else
        {
        	request.setAttribute("dspace.layout.feedref", "NONE" );
        }

        // Now include the header
        try
        {
            HttpServletResponse response = (HttpServletResponse) pageContext
                    .getResponse();

            // Set headers to prevent browser caching, if appropriate
            if ((noCache != null) && noCache.equalsIgnoreCase("true"))
            {
                response.addDateHeader("expires", 1);
                response.addHeader("Pragma", "no-cache");
                response.addHeader("Cache-control", "no-store");
            }

            // Ensure the HTTP header will declare that UTF-8 is used
            // in the response.
            response.setContentType("text/html; charset=UTF-8");

            ServletConfig config = pageContext.getServletConfig();

            RequestDispatcher rd = config.getServletContext()
                    .getRequestDispatcher(header);

            rd.include(request, response);
        }
        catch (IOException ioe)
        {
            throw new JspException("Got IOException: " + ioe);
        }
        catch (ServletException se)
        {
            log.warn("Exception", se.getRootCause());
            throw new JspException("Got ServletException: " + se);
        }

        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException
    {
        // Footer file to use
        String footer = "/layout/footer-default.jsp";

        // Choose default flavour unless one is specified
        if (style != null)
        {
            footer = "/layout/footer-" + style.toLowerCase() + ".jsp";
        }

        try
        {
            // Ensure body is included before footer
            pageContext.getOut().flush();

            // Context objects
            ServletRequest request = pageContext.getRequest();
            ServletResponse response = pageContext.getResponse();
            ServletConfig config = pageContext.getServletConfig();

            if (sidebar != null)
            {
                request.setAttribute("dspace.layout.sidebar", sidebar);
            }

            RequestDispatcher rd = config.getServletContext()
                    .getRequestDispatcher(footer);

            rd.include(request, response);
        }
        catch (ServletException se)
        {
            throw new JspException("Got ServletException: " + se);
        }
        catch (IOException ioe)
        {
            throw new JspException("Got IOException: " + ioe);
        }

        return EVAL_PAGE;
    }

    /**
     * Get the value of title.
     * 
     * @return Value of title.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the value of title.
     * 
     * @param v
     *            Value to assign to title.
     */
    public void setTitle(String v)
    {
        this.title = v;
    }

    /**
     * @return Returns the titleKey.
     */
    public String getTitlekey()
    {
        return titleKey;
    }
    
    /**
     * @param titleKey The titleKey to set.
     */
    public void setTitlekey(String titleKey)
    {
        this.titleKey = titleKey;
    }
    
    /**
     * Get the value of navbar.
     * 
     * @return Value of navbar.
     */
    public String getNavbar()
    {
        return navbar;
    }

    /**
     * Set the value of navbar.
     * 
     * @param v
     *            Value to assign to navbar.
     */
    public void setNavbar(String v)
    {
        this.navbar = v;
    }

    /**
     * Get the value of locbar.
     * 
     * @return Value of locbar.
     */
    public String getLocbar()
    {
        return locbar;
    }

    /**
     * Set the value of locbar.
     * 
     * @param v
     *            Value to assign to locbar.
     */
    public void setLocbar(String v)
    {
        this.locbar = v;
    }

    /**
     * Get the value of parentTitle.
     * 
     * @return Value of parentTitle.
     */
    public String getParenttitle()
    {
        return parentTitle;
    }

    /**
     * Set the value of parent.
     * 
     * @param v
     *            Value to assign to parent.
     */
    public void setParenttitle(String v)
    {
        this.parentTitle = v;
    }

    /**
     * get parent title key (from message dictionary)
     * 
     * @return Returns the parentTitleKey.
     */
    public String getParenttitlekey()
    {
        return parentTitleKey;
    }

    /**
     * set parent title key (from message dictionary)
     * 
     * @param parentTitleKey The parentTitleKey to set.
     */
    public void setParenttitlekey(String parentTitleKey)
    {
        this.parentTitleKey = parentTitleKey;
    }

    /**
     * Get the value of parentlink.
     * 
     * @return Value of parentlink.
     */
    public String getParentlink()
    {
        return parentLink;
    }

    /**
     * Set the value of parentlink.
     * 
     * @param v
     *            Value to assign to parentlink.
     */
    public void setParentlink(String v)
    {
        this.parentLink = v;
    }

    /**
     * Get the value of style.
     * 
     * @return Value of style.
     */
    public String getStyle()
    {
        return style;
    }

    /**
     * Set the value of style.
     * 
     * @param v
     *            Value to assign to style.
     */
    public void setStyle(String v)
    {
        this.style = v;
    }

    /**
     * Get the value of sidebar.
     * 
     * @return Value of sidebar.
     */
    public String getSidebar()
    {
        return sidebar;
    }

    /**
     * Set the value of sidebar.
     * 
     * @param v
     *            Value to assign to sidebar.
     */
    public void setSidebar(String v)
    {
        this.sidebar = v;
    }

    /**
     * Get the value of sidebar.
     * 
     * @return Value of sidebar.
     */
    public String getNocache()
    {
        return noCache;
    }

    /**
     * Set the value of sidebar.
     * 
     * @param v
     *            Value to assign to sidebar.
     */
    public void setNocache(String v)
    {
        this.noCache = v;
    }
    
    /**
     * Get the value of feedData.
     * 
     * @return Value of feedData.
     */
    public String getFeedData()
    {
        return feedData;
    }

    /**
     * Set the value of feedData.
     * 
     * @param v
     *            Value to assign to feedData.
     */
    public void setFeedData(String v)
    {
        this.feedData = v;
    }

    public void release()
    {
        style = null;
        title = null;
        sidebar = null;
        navbar = null;
        locbar = null;
        parentTitle = null;
        parentLink = null;
        noCache = null;
        feedData = null;
    }
}
