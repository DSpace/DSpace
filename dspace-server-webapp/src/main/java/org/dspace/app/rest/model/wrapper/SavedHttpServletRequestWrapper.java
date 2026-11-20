/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;



/**
 * A custom {@link HttpServletRequestWrapper} that allows to access to request fields in async way,
 * even after the request is not available anymore, i.e. it's been recycled
 */
public class SavedHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, List<String>> headers;
    private final Map<Boolean, SavedHttpSessionWrapper> sessionCache;
    private final String requestURI;
    private final String remoteAddr;
    private final String remoteHost;
    private final String queryString;

    /**
     * Create an instance of {@link SavedHttpServletRequestWrapper}
     *
     * @param request the original request
     */
    public SavedHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        headers = initHeaders(request);
        sessionCache = initSessionCache();
        requestURI = super.getRequestURI();
        remoteAddr = super.getRemoteAddr();
        remoteHost = super.getRemoteHost();
        queryString = super.getQueryString();
    }

    private Map<Boolean, SavedHttpSessionWrapper> initSessionCache() {
        Map<Boolean, SavedHttpSessionWrapper> savedSessionCache = new HashMap<>();
        HttpSession session = super.getSession(false);
        SavedHttpSessionWrapper savedHttpSession = new SavedHttpSessionWrapper(session);
        savedSessionCache.put(Boolean.FALSE, session != null ? savedHttpSession : null);
        savedSessionCache.put(Boolean.TRUE,
                session == null ? new SavedHttpSessionWrapper(super.getSession(true)) : savedHttpSession);
        return savedSessionCache;
    }

    private Map<String, List<String>> initHeaders(HttpServletRequest request) {
        final Map<String, List<String>> headers;
        headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, Collections.list(super.getHeaders(headerName)));
        }
        return headers;
    }

    @Override
    public String getHeader(String name) {
        if (headers.get(name) != null && !headers.get(name).isEmpty()) {
            return headers.get(name).get(0);
        }
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return sessionCache.get(create);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }
}
