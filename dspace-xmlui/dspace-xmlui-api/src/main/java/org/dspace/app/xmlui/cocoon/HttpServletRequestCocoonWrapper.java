/*
 * HttpServletRequestCocoonWrapper.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2006/03/13 17:19:39 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

/**
 * This is a wrapper class that translates a Cocoon request object back 
 * into an HttpServletRequest object. The main purpose of this class is to 
 * support form encoding in libraries that require a real HttpServletRequest 
 * object. If they are given the real request object then any parameters they 
 * obtain from it will not take into account the form encoding. Thus this 
 * method, by proxing everything back through the cocoon object will not 
 * be hindered by this problem.  
 * 
 * 
 * Some methods are unsupported, see below for a list.
 * getCookies(),getInputStream(),getParameterMap(),getReader(),
 * getRealPath(String arg0),getRequestDispatcher(String arg0)
 * 
 * @author Scott Phillips
 */

public class HttpServletRequestCocoonWrapper implements HttpServletRequest{

	private Request cocoonRequest;
	//private HttpServletRequest realRequest;
	
	public HttpServletRequestCocoonWrapper(Map objectModel) {
		
		// Get the two requests objects the cocoon one, and the real request object as a fall back.
		this.cocoonRequest = ObjectModelHelper.getRequest(objectModel);
		
		// If the real request is needed in the future to fall back to when the
		// cocoon request object dosn't support those methods use the following line.
		//this.realRequest = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
	}
	
	public String getAuthType() {
		return this.cocoonRequest.getAuthType();
	}

	public String getContextPath() {
		return this.cocoonRequest.getContextPath();
	}

	public Cookie[] getCookies() {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support cookies.");
	}

	public long getDateHeader(String arg0) {
		return this.cocoonRequest.getDateHeader(arg0);
	}

	public String getHeader(String arg0) {
		return this.cocoonRequest.getHeader(arg0);
	}

	public Enumeration getHeaderNames() {
		return this.cocoonRequest.getHeaderNames();
	}

	public Enumeration getHeaders(String arg0) {
		return this.cocoonRequest.getHeaders(arg0);
	}

	public int getIntHeader(String arg0) {
		Enumeration e = getHeaders(arg0);
		if (e.hasMoreElements())
		{
			Object v = e.nextElement();
			if (v instanceof String)
			{
				try {
					return Integer.valueOf((String)v);
				} catch (NumberFormatException nfe)
				{
					// do nothing
				}
			}
		}
		return -1;
	}

	public String getMethod() {
		return this.cocoonRequest.getMethod();
	}

	public String getPathInfo() {
		return this.cocoonRequest.getPathInfo();
	}

	public String getPathTranslated() {
		return this.cocoonRequest.getPathTranslated();
	}

	public String getQueryString() {
		return this.cocoonRequest.getQueryString();
	}

	public String getRemoteUser() {
		return this.getRemoteUser();
	}

	public String getRequestURI() {
		return this.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return new StringBuffer(this.cocoonRequest.getRequestURI());
	}

	public String getRequestedSessionId() {
		return this.cocoonRequest.getRequestedSessionId();
	}

	public String getServletPath() {
		return this.getServletPath();
	}

	public HttpSession getSession() {
		return this.getSession();
	}

	public HttpSession getSession(boolean arg0) {
		return this.getSession(arg0);
	}

	public Principal getUserPrincipal() {
		return this.cocoonRequest.getUserPrincipal();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return this.cocoonRequest.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return this.cocoonRequest.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return this.cocoonRequest.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdValid() {
		return this.cocoonRequest.isRequestedSessionIdValid();
	}

	public boolean isUserInRole(String arg0) {
		return this.cocoonRequest.isUserInRole(arg0);
	}

	public Object getAttribute(String arg0) {
		return this.cocoonRequest.getAttribute(arg0);
	}

	public Enumeration getAttributeNames() {
		return this.cocoonRequest.getAttributeNames();
	}

	public String getCharacterEncoding() {
		return this.cocoonRequest.getCharacterEncoding();
	}

	public int getContentLength() {
		return this.cocoonRequest.getContentLength();
	}

	public String getContentType() {
		return this.cocoonRequest.getContentType();
	}

	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support getInputStream().");
	}

	public Locale getLocale() {
		return this.cocoonRequest.getLocale();
	}

	public Enumeration getLocales() {
		return this.cocoonRequest.getLocales();
	}

	public String getParameter(String arg0) {
		return this.cocoonRequest.getParameter(arg0);
	}

	public Map getParameterMap() {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support getParameterMap().");
	}

	public Enumeration getParameterNames() {
		return this.cocoonRequest.getParameterNames();
	}

	public String[] getParameterValues(String arg0) {
		return this.cocoonRequest.getParameterValues(arg0);
	}

	public String getProtocol() {
		return this.cocoonRequest.getProtocol();
	}

	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support getReader().");
	}

	public String getRealPath(String arg0) {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support getRealPath().");
	}

	public String getRemoteAddr() {
		return this.cocoonRequest.getRemoteAddr();
	}

	public String getRemoteHost() {
		return this.cocoonRequest.getRemoteHost();
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new UnsupportedOperationException("HttpRequestCocoonWrapper does not support getRequestDispatcher(arg0).");
	}

	public String getScheme() {
		return this.cocoonRequest.getScheme();
	}

	public String getServerName() {
		return this.cocoonRequest.getServerName();
	}

	public int getServerPort() {
		return this.cocoonRequest.getServerPort();
	}

	public boolean isSecure() {
		return this.cocoonRequest.isSecure();
	}

	public void removeAttribute(String arg0) {
		this.cocoonRequest.removeAttribute(arg0);
	}

	public void setAttribute(String arg0, Object arg1) {
		this.cocoonRequest.setAttribute(arg0, arg1);
	}

	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		this.cocoonRequest.setCharacterEncoding(arg0);
	}
	
	
	

}
