/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions.model;

import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


/**
 * This is a special HTTP session object that stands in for a real one
 * when sessions are not initiated or associated by HTTP requests.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@SuppressWarnings("deprecation")
public final class InternalHttpSession implements HttpSession {

    private final String id;
    private long lastAccessedTime = System.currentTimeMillis();
    private long creationTime = System.currentTimeMillis();
    private int maxInactiveInternal = 1800;
    private boolean invalidated = false;

    private ConcurrentHashMap<String, Object> attributes = null;
    private ConcurrentHashMap<String, Object> getAttributes() {
        if (this.attributes == null) {
            this.attributes = new ConcurrentHashMap<String, Object>();
        }
        return this.attributes;
    }

    public InternalHttpSession() {
        this.id = UUID.randomUUID().toString();
    }

    private void checkInvalidated() {
        if (invalidated) {
            throw new IllegalStateException("This session is no longer valid");
        }
    }

    @Override
    public String toString() {
        return "internalSession:" + this.id + ":" + this.creationTime + ":" + this.invalidated + ":" + super.toString();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        checkInvalidated();
        return getAttributes().get(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
        checkInvalidated();
        return getAttributes().keys();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        checkInvalidated();
        return creationTime;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getId()
     */
    public String getId() {
        checkInvalidated();
        return id;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        checkInvalidated();
        return lastAccessedTime;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        checkInvalidated();
        return maxInactiveInternal;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    public ServletContext getServletContext() {
        checkInvalidated();
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getSessionContext()
     */
    public HttpSessionContext getSessionContext() {
        checkInvalidated();
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     */
    public Object getValue(String name) {
        checkInvalidated();
        return getAttributes().get(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getValueNames()
     */
    public String[] getValueNames() {
        checkInvalidated();
        Set<String> names = getAttributes().keySet();
        return names.toArray(new String[names.size()]);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        invalidated = true;
        if (this.attributes != null) {
            this.attributes.clear();
            this.attributes = null;
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#isNew()
     */
    public boolean isNew() {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     */
    public void putValue(String name, Object value) {
        checkInvalidated();
        getAttributes().put(name, value);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        checkInvalidated();
        getAttributes().remove(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     */
    public void removeValue(String name) {
        checkInvalidated();
        getAttributes().remove(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object value) {
        checkInvalidated();
        getAttributes().put(name, value);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int interval) {
        checkInvalidated();
        this.maxInactiveInternal = interval;
    }

}
