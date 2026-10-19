/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.statistics.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.core.Utils;

/**
 * A mock request for testing.
 *
 * @author mwood
 */
public class DummyHttpServletRequest implements HttpServletRequest {
    private String agent = null;

    private String address = null;

    private String remoteHost = null;

    private Map<String, List<String>> headers = new HashMap<>();

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setRemoteHost(String host) {
        this.remoteHost = host;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#changeSessionId
     */
    @Override
    public String changeSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getAuthType()
     */
    @Override
    public String getAuthType() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
     */
    @Override
    public String getContextPath() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getCookies()
     */
    @Override
    public Cookie[] getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    @Override
    public long getDateHeader(String arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Add a request header to this dummy request
     * @param headerName The name of the header to add
     * @param headerValue The value of the header
     */
    public void addHeader(String headerName, String headerValue) {
        List<String> values = headers.computeIfAbsent(headerName, k -> new ArrayList<>());
        values.add(headerValue);
    }
    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getDispatcherType()
     */
    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getRequestId()
     */
    @Override
    public String getRequestId() {
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getProtocolRequestId()
     */
    @Override
    public String getProtocolRequestId() {
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getServletConnection()
     */
    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String key) {
        if ("User-Agent".equals(key)) {
            return agent;
        } else {
            return CollectionUtils.isEmpty(headers.get(key)) ? null : headers.get(key).get(0);
        }
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getHeaderNames()
     */
    @Override
    public Enumeration getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
     */
    @Override
    public Enumeration getHeaders(String arg0) {
        return Collections.enumeration(Utils.emptyIfNull(headers.get(arg0)));
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    @Override
    public int getIntHeader(String arg0) {
        return headers.containsKey(arg0) ? Integer.parseInt(getHeader(arg0)) : -1;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getMethod()
     */
    @Override
    public String getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getPathInfo()
     */
    @Override
    public String getPathInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getPathTranslated()
     */
    @Override
    public String getPathTranslated() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getQueryString()
     */
    @Override
    public String getQueryString() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getRemoteUser()
     */
    @Override
    public String getRemoteUser() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getRequestURI()
     */
    @Override
    public String getRequestURI() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getRequestURL()
     */
    @Override
    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getRequestedSessionId()
     */
    @Override
    public String getRequestedSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getServletPath()
     */
    @Override
    public String getServletPath() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getSession()
     */
    @Override
    public HttpSession getSession() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getSession(boolean)
     */
    @Override
    public HttpSession getSession(boolean arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getUserPrincipal()
     */
    @Override
    public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#authenticate(jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) {
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#login(java.lang.String,java.lang.String)
     */
    @Override
    public void login(String s, String s1) {
        return;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#logout()
     */
    @Override
    public void logout() {
        return;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getPart(java.lang.String)
     */
    @Override
    public Part getPart(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#getParts()
     */
    @Override
    public Collection<Part> getParts() {
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#upgrade(java.lang.Class<T>)
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
     */
    @Override
    public boolean isUserInRole(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getAttributeNames()
     */
    @Override
    public Enumeration getAttributeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getCharacterEncoding()
     */
    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getContentLength()
     */
    @Override
    public int getContentLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getContentLengthLong()
     */
    @Override
    public long getContentLengthLong() {
        return 0;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getContentType()
     */
    @Override
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getInputStream()
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getLocale()
     */
    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getLocales()
     */
    @Override
    public Enumeration getLocales() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getParameterMap()
     */
    @Override
    public Map getParameterMap() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getParameterNames()
     */
    @Override
    public Enumeration getParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    @Override
    public String[] getParameterValues(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getProtocol()
     */
    @Override
    public String getProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getReader()
     */
    @Override
    public BufferedReader getReader() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getRemoteAddr()
     */
    @Override
    public String getRemoteAddr() {
        return address;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getRemoteHost()
     */
    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getScheme()
     */
    @Override
    public String getScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getServerName()
     */
    @Override
    public String getServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getServerPort()
     */
    @Override
    public int getServerPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#isSecure()
     */
    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String arg0, Object arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */
    @Override
    public void setCharacterEncoding(String arg0)
        throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#startAsync
     */
    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#startAsync(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)
     */
    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException {
        throw new IllegalStateException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#isAsyncStarted
     */
    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#isAsyncSupported
     */
    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getAsyncContext
     */
    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public int getRemotePort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.ServletRequest#getServletContext
     */
    @Override
    public ServletContext getServletContext() {
        return null;
    }

}
