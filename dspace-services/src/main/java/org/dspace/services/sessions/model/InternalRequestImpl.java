/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.services.model.Request;

public final class InternalRequestImpl extends AbstractRequestImpl implements Request {

    private Map<String, Object> attributes = new HashMap<String, Object>();

    public InternalRequestImpl() {
    }

    @Override
    public ServletRequest getServletRequest() {
        return null;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return null;
    }

    @Override
    public ServletResponse getServletResponse() {
        return null;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }
}
