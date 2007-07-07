/*
 * RequestInfo.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2005/04/20 14:23:00 $
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
package org.dspace.app.xmlui.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Stores information about an HTTP request. This is used so that the request
 * can be replicated during a later request, once authentication has
 * successfully occurred. 
 * 
 * This class is the same as found in the JSPUI however a few extra methods were added to
 * remember the original path information (info,translated, uri, url, etc..) that coocoon
 * needs inorder to be able to resume a request.
 * 
 * @author Robert Tansley
 * @author Scott Phillips
 * @version $Revision: 1.5 $
 */
public class RequestInfo
{
	
	/** Request characteristics that are stored for later resumption */
	private final String authType;
	private final String contextPath;
	private final String method;
	private final String pathInfo;
	private final String pathTranslated;
	private final String queryString;
	private final String requestURI;
	private final StringBuffer requestURL;
	private final String servletPath;
	private final String scheme;
	private final boolean secure;
	
	private final Map<String,String[]> parameters;

    /**
     * Construct a request info object storing information about the given
     * request
     * 
     * @param request
     *            the request to get information from
     */
    public RequestInfo(HttpServletRequest request)
    {
    	this.authType = request.getAuthType();
    	this.contextPath = request.getContextPath();
    	this.method = request.getMethod();
    	this.pathInfo = request.getPathInfo();
    	this.pathTranslated = request.getPathTranslated();
    	this.queryString = request.getQueryString();
    	this.requestURI = request.getRequestURI();
    	this.requestURL = request.getRequestURL();
    	this.servletPath = request.getServletPath();
    	this.scheme = request.getScheme();
    	this.secure = request.isSecure();
    	
    	// Copy the parameters
    	this.parameters = new HashMap<String,String[]>();
    	Enumeration enumeration = request.getParameterNames();
    	while (enumeration.hasMoreElements())
    	{
    		String key = (String) enumeration.nextElement();
    		String[] values = request.getParameterValues(key);
    		this.parameters.put(key, values);
    	}
    }

    /**
     * Return the servlet path that this request is for.
     */
    public String getServletPath()
    {
    	return this.servletPath;
    }
    
    /**
     * Wrap an incoming request to make it look like the request that the
     * constructor was called with
     * 
     * @param request
     *            the request to wrap
     * 
     * @return a wrapper around the request passed into this method, wrapped so
     *         that it looks like the request passed into the constructor
     */
    public HttpServletRequest wrapRequest(HttpServletRequest request)
    {
        return new RequestWrapper(request);
    }

    /**
     * Our own flavour of HTTP request wrapper, that uses information from this
     * RequestInfo object
     */
    class RequestWrapper extends HttpServletRequestWrapper
    {
    	
    	public RequestWrapper(HttpServletRequest request) {
    		super(request);
    	}
    	
    	public String getAuthType() {
    		return authType;
    	}

    	public String getContextPath() {
    		return contextPath;
    	}

    	public String getMethod() {
    		return method;
    	}

    	public String getPathInfo() {
    		return pathInfo;
    	}

    	public String getPathTranslated() {
    		return pathTranslated;
    	}

    	public String getQueryString() {
    		return queryString;
    	}

    	public String getRequestURI() {
    		return requestURI;
    	}

    	public StringBuffer getRequestURL() {
    		return requestURL;
    	}

    	public String getServletPath() {
    		return servletPath;
    	}

    	public String getParameter(String arg0) {	
    		String[] values = parameters.get(arg0);
    		
    		if (values == null || values.length == 0)
    			return null;
    		else
    			return values[0];
    	}

    	public Map getParameterMap() {
    		return parameters;
    	}

    	public Enumeration getParameterNames() {
    		Iterator parameterIterator = parameters.keySet().iterator();
    		return new EnumIterator(parameterIterator);
    	}

    	public String[] getParameterValues(String arg0) {
    		return parameters.get(arg0);
    	}
    	
    	public String getScheme() {
    		return scheme;
    	}

    	public boolean isSecure() {
    		return secure;
    	}

    	
//    	Here are other prototypes that are included in the 2.3 servlet spec:
//    	
//    	public Cookie[] getCookies()
//    	public long getDateHeader(String arg0)
//    	public String getHeader(String arg0)
//    	public Enumeration getHeaderNames()
//    	public Enumeration getHeaders(String arg0)
//    	public int getIntHeader(String arg0)
//    	public String getRemoteUser() 
//    	public String getRequestedSessionId() 
//    	public HttpSession getSession() 
//    	public HttpSession getSession(boolean arg0) 
//    	public Principal getUserPrincipal() 
//    	public boolean isRequestedSessionIdFromCookie() 
//    	public boolean isRequestedSessionIdFromURL() 
//    	public boolean isRequestedSessionIdFromUrl() 
//    	public boolean isRequestedSessionIdValid() 
//    	public boolean isUserInRole(String arg0) 
//    	public Object getAttribute(String arg0) 
//    	public Enumeration getAttributeNames() 
//    	public String getCharacterEncoding() 
//    	public int getContentLength() 
//    	public String getContentType() 
//    	public ServletInputStream getInputStream() throws IOException 
//    	public Locale getLocale() 
//    	public Enumeration getLocales() 
//    	public String getProtocol() 
//    	public BufferedReader getReader() throws IOException 
//    	public String getRealPath(String arg0) 
//    	public String getRemoteAddr() 
//    	public String getRemoteHost() 
//    	public RequestDispatcher getRequestDispatcher(String arg0) 
//    	public String getServerName() 
//    	public int getServerPort() 
//    	public void removeAttribute(String arg0) 
//    	public void setAttribute(String arg0, Object arg1) 
//    	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException 
    	
    	/**
         * This class converts an interator into an enumerator. This is done
         * because we have the parameters as a Map (JDK 1.2 style), but for some
         * weird reason the HttpServletRequest interface returns an Enumeration
         * from getParameterNames() (JDK1.0 style.) JDK apparently offers no way
         * of simply changing between the new styles.
         */
        class EnumIterator implements Enumeration
        {
            private Iterator iterator;

            public EnumIterator(Iterator i)
            {
                iterator = i;
            }

            public boolean hasMoreElements()
            {
                return iterator.hasNext();
            }

            public Object nextElement()
            {
                return iterator.next();
            }
        }
    }
}
