/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Request {
    public String getRequestId();

    public Object getAttribute(String name);

    public void setAttribute(String name, Object o);

    public ServletRequest getServletRequest();
    
    public HttpServletRequest getHttpServletRequest();

    public ServletResponse getServletResponse();

    public HttpServletResponse getHttpServletResponse();
}
