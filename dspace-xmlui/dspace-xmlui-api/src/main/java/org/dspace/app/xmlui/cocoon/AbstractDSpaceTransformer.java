/*
 * AbstractDSpaceTransformer.java
 *
 * Version: $Revision: 1.14 $
 *
 * Date: $Date: 2006/05/02 05:30:55 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.objectmanager.DSpaceObjectManager;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.AbstractWingTransformer;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * @author Scott Phillips
 */
public abstract class AbstractDSpaceTransformer extends AbstractWingTransformer
        implements DSpaceTransformer
{

    private static final String NAME_TRIM = "org.dspace.app.xmlui.";

    protected Map objectModel;

    protected Context context;

    protected String contextPath;

    protected String servletPath;

    protected String sitemapURI;

    protected String url;
    
    protected Parameters parameters;
    
    protected EPerson eperson;
    
    protected WebContinuation knot;
    
    // Only access this through getObjectManager, so that we don't have to create one if we don't want too.
    private ObjectManager objectManager;

    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        this.objectModel = objectModel;
        this.parameters = parameters;
        try
        {
            this.context = ContextUtil.obtainContext(objectModel);
            this.eperson = context.getCurrentUser();
            Request request = ObjectModelHelper.getRequest(objectModel);
            this.contextPath = request.getContextPath();
            if (contextPath == null)
            	contextPath = "/";
            
            this.servletPath = request.getServletPath();
            this.sitemapURI = request.getSitemapURI(); 
            this.knot = FlowHelper.getWebContinuation(objectModel);
        }
        catch (SQLException sqle)
        {
            handleException(sqle);
        }
        
        // Initialize the Wing framework.
        try
        {
            this.setupWing();
        }
        catch (WingException we)
        {
            throw new ProcessingException(we);
        }
    }

    protected void handleException(Exception e) throws SAXException
    {
        throw new SAXException(
                "An error was encountered while processing the '"+this.getComponentName()+"' Wing based component: "
                        + this.getClass().getName(), e);
    }

    /** What to add at the end of the body */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // Do nothing
    }

    /** What to add to the options list */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // Do nothing
    }

    /** What user metadata to add to the document */
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Do nothing
    }

    /** What page metadata to add to the document */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Do nothing
    }
    
    public ObjectManager getObjectManager() 
    {
        if (this.objectManager == null)
            this.objectManager = new DSpaceObjectManager();
        return this.objectManager;
    }
    
    /** What is a unique name for this component? */
    public String getComponentName()
    {
        String name = this.getClass().getName();
        if (name.startsWith(NAME_TRIM))
            name = name.substring(NAME_TRIM.length());
        return name;
    }

    /**
     * Encode the given string for URL transmission.
     * 
     * @param unencodedString
     *            The unencoded string.
     * @return The encoded string
     */
    public static String URLEncode(String unencodedString) throws UIException
    {
    	if (unencodedString == null)
    		return "";
    	
        try
        {
            return URLEncoder.encode(unencodedString,Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }

    }

    /**
     * Decode the given string from URL transmission.
     * 
     * @param encodedString
     *            The encoded string.
     * @return The unencoded string
     */
    public static String URLDecode(String encodedString) throws UIException
    {
    	if (encodedString == null)
    		return null;
    	
        try
        {
            return URLDecoder.decode(encodedString, Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new UIException(uee);
        }

    }

    /**
     * Generate a URL for the given base URL with the given parameters. This is
     * a convenance method to make it easier to generate URL refrences with
     * parameters.
     * 
     * Example
     * Map<String,String> parameters = new Map<String,String>();
     * parameters.put("arg1","value1");
     * parameters.put("arg2","value2");
     * parameters.put("arg3","value3");
     * String url = genrateURL("/my/url",parameters);
     * 
     * would result in the string:
     * url == "/my/url?arg1=value1&arg2=value2&arg3=value3"
     * 
     * @param baseURL The baseURL without any parameters.
     * @param parameters The parameters to be encoded on in the URL.
     * @return The parameterized Post URL.
     */
    public static String generateURL(String baseURL,
            Map<String, String> parameters)
    {
        boolean first = true;
        for (String key : parameters.keySet())
        {
            if (first)
            {
                baseURL += "?";
                first = false;
            }
            else
            {
                baseURL += "&";
            }

            baseURL += key + "=" + parameters.get(key);
        }

        return baseURL;
    }
    
    
    /**
     * Recyle
     */
    public void recycle() {
    	this.objectModel = null;
        this.context = null;
        this.contextPath = null;
        this.servletPath = null;
        this.sitemapURI = null;
        this.url=null;
        this.parameters=null;
        this.eperson=null;
        this.knot=null;
        this.objectManager=null;
    	super.recycle();
    }

    /**
     * Dispose
     */
    public void dispose() {
    	this.objectModel = null;
        this.context = null;
        this.contextPath = null;
        this.servletPath = null;
        this.sitemapURI = null;
        this.url=null;
        this.parameters=null;
        this.eperson=null;
        this.knot=null;
        this.objectManager=null;
    	super.dispose();
    }
    
}
