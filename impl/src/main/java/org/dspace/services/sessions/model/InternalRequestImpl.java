package org.dspace.services.sessions.model;

import org.dspace.services.model.Request;
import org.dspace.services.model.Session;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class InternalRequestImpl extends AbstractRequestImpl implements Request {

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private Session session = new SessionImpl();

    public InternalRequestImpl() {
    }

    public ServletRequest getServletRequest() {
        return null;
    }

    public HttpServletRequest getHttpServletRequest() {
        return null;
    }

    public Session getSession() {
        return session;
    }

    public ServletResponse getServletResponse() {
        return null;
    }

    public HttpServletResponse getHttpServletResponse() {
        return null;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }
}
