package org.dspace.services.model;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Request {
    public String getRequestId();

    public Session getSession();

    public Object getAttribute(String name);

    public void setAttribute(String name, Object o);

    public ServletRequest getServletRequest();
    
    public HttpServletRequest getHttpServletRequest();

    public ServletResponse getServletResponse();

    public HttpServletResponse getHttpServletResponse();
}
