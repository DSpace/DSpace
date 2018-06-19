/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * This class is the same as found in the JSPUI; however, a few extra methods were added to
 * remember the original path information (info, translated, uri, url, etc..) that Cocoon
 * needs in order to be able to resume a request.
 * 
 * @author Robert Tansley
 * @author Scott Phillips
 * @version $Revision$
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
     * @return the path.
     */
    public String getServletPath()
    {
    	return this.servletPath;
    }

    /**
     * Return the actual path that this request is for.
     * @return the path.
     */
    public String getActualPath()
    {
    	return this.pathInfo + ((queryString == null || queryString.length() == 0) ? "" : "?"+queryString);
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
    	
        @Override
    	public String getAuthType() {
    		return authType;
    	}

        @Override
    	public String getContextPath() {
    		return contextPath;
    	}

        @Override
    	public String getMethod() {
    		return method;
    	}

        @Override
    	public String getPathInfo() {
    		return pathInfo;
    	}

        @Override
    	public String getPathTranslated() {
    		return pathTranslated;
    	}

        @Override
    	public String getQueryString() {
    		return queryString;
    	}

        @Override
    	public String getRequestURI() {
    		return requestURI;
    	}

        @Override
    	public StringBuffer getRequestURL() {
    		return requestURL;
    	}

        @Override
    	public String getServletPath() {
    		return servletPath;
    	}

        @Override
    	public String getParameter(String arg0) {	
    		String[] values = parameters.get(arg0);
    		
    		if (values == null || values.length == 0)
            {
                return null;
            }
    		else
            {
                return values[0];
            }
    	}

        @Override
    	public Map getParameterMap() {
    		return parameters;
    	}

        @Override
    	public Enumeration getParameterNames() {
    		Iterator parameterIterator = parameters.keySet().iterator();
    		return new EnumIterator(parameterIterator);
    	}

        @Override
    	public String[] getParameterValues(String arg0) {
    		return parameters.get(arg0);
    	}
    	
        @Override
    	public String getScheme() {
    		return scheme;
    	}

        @Override
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
         * This class converts an iterator into an enumerator. This is done
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

            @Override
            public boolean hasMoreElements()
            {
                return iterator.hasNext();
            }

            @Override
            public Object nextElement()
            {
                return iterator.next();
            }
        }
    }
}
