/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions.model;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.dspace.services.model.Session;

/**
 * Represents a users session (login session) in the system.  Can hold 
 * some additional attributes as needed, but the underlying
 * implementation may limit the number and size of attributes to ensure
 * that session replication is not impacted negatively.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@SuppressWarnings("deprecation")
public final class SessionImpl implements Session {

    // keys for things stored in the session
    public static final String SESSION_ID = "dspaceSessionId";
    public static final String USER_ID = "userId";
    public static final String USER_EID = "userEid";
    public static final String SERVER_ID = "serverId";
    public static final String HOST_IP = "originatingHostIP";
    public static final String HOST_NAME = "originatingHostName";

    /**
     * This is the only thing that is actually replicated across the cluster.
     */
    private transient HttpSession httpSession;

    /**
     * Make a session that is associated with the current HTTP request.
     *
     * @param request current request
     */
    public SessionImpl(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cannot create a session without an http request");
        }
        this.httpSession = request.getSession(); // establish the session
        setKeyAttribute(HOST_IP, request.getRemoteAddr());
        setKeyAttribute(HOST_NAME, request.getRemoteHost());
    }

    /**
     * Make a session which is not associated with the current HTTP request.
     */
    public SessionImpl() {
        // creates a new internal http session that is not cached anywhere
        this.httpSession = new InternalHttpSession();
        try {
            InetAddress i4 = Inet4Address.getLocalHost();
            setKeyAttribute(HOST_IP, i4.getHostAddress()); // IP address
            setKeyAttribute(HOST_NAME, i4.getHostName());
        } catch (UnknownHostException e) {
            // could not get address
            setKeyAttribute(HOST_IP, "10.0.0.1"); // set a fake one I guess
        }

    }

    /**
     * Set the sessionId.  Normally this should not probably happen much.
     * @param sessionId the session ID
     */
    public void setSessionId(String sessionId) {
        if (! isAttributeSet(SESSION_ID)) {
            if (isBlank(sessionId)) {
                // just use the http session id
                setKeyAttribute(SESSION_ID, this.httpSession.getId());
            } else {
                setKeyAttribute(SESSION_ID, sessionId);
            }
        }
    }

    /**
     * Set the userId and userEid.  This should only happen when
     * re-binding the session or clearing the associated user.
     * If userId is null then user is cleared.
     *
     * @param userId the user ID
     * @param userEid the user EID
     */
    public void setUserId(String userId, String userEid) {
        if (isBlank(userId)) {
            removeKeyAttribute(USER_ID);
            removeKeyAttribute(USER_EID);
        } else {
            setKeyAttribute(USER_ID, userId);
            setKeyAttribute(USER_EID, userEid);
        }
    }

    /**
     * Set the DSpace serverId which originated this session.
     *
     * @param serverId the serverId
     */
    public void setServerId(String serverId) {
        setKeyAttribute(SERVER_ID, serverId);
    }

    /**
     * Are all required values set?  Notice that the sense of the test 
     * is the complement of what the name of this method implies.
     *
     * @return true if this session already has all the required values
     * needed to complete it.  This means the serverId and other values
     * in the session are already set.
     */
    public boolean isIncomplete() {
        boolean complete = false;
        if (isAttributeSet(SERVER_ID) 
                && isAttributeSet(SESSION_ID)
                && isAttributeSet(HOST_IP)) {
            complete = true;
        }
        return ! complete;
    }

    /**
     * @param key the attribute key
     * @return true if the attribute is set
     */
    public boolean isAttributeSet(String key) {
        return getKeyAttribute(key) != null;
    }

    /**
     * @return true if this session is invalidated
     */
    public boolean isInvalidated() {
        boolean invalid = true;
        if (this.httpSession != null) {
            try {
                this.httpSession.getCreationTime();
                invalid = false;
            } catch (IllegalStateException e) {
                invalid = true;
            }
        } else {
            // no httpsession
            invalid = false;
        }
        return invalid;
    }

    /**
     * Handles the general setting of things in the session.
     * Use this to build other set methods.
     * Handles checking the session is still valid, and
     * the checking for null values in the value and key.
     *
     * @param key the key to use
     * @param value the value to set
     * @return true if the value was set, false if cleared or failure
     * @throws IllegalArgumentException if the key is null
     */
    protected boolean setKeyAttribute(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("session attribute key cannot be null");
        }
        boolean wasSet = false;
        if (! isInvalidated()) {
            if (isBlank(value)) {
                this.httpSession.removeAttribute(key);
            } else {
                this.httpSession.setAttribute(key, value);
                wasSet = true;
            }
        }
        return wasSet;
    }

    /**
     * Handles the general getting of things from the session.
     * Use this to build other set methods.
     * Checks that the session is still valid.
     *
     * @param key the key to use
     * @return the value OR null if not found
     * @throws IllegalArgumentException if the key is null
     */
    protected String getKeyAttribute(String key) {
        if (key == null) {
            throw new IllegalArgumentException("session attribute key cannot be null");
        }
        String value = null;
        if (! isInvalidated()) {
            value = (String) this.httpSession.getAttribute(key);
        }
        return value;
    }

    /**
     * Handles removal of attributes and related checks.
     *
     * @param key the key to use
     * @throws IllegalArgumentException if the key is null
     */
    protected void removeKeyAttribute(String key) {
        if (key == null) {
            throw new IllegalArgumentException("session attribute key cannot be null");
        }
        if (! isInvalidated()) {
            this.httpSession.removeAttribute(key);
        }
    }

    @Override
    public boolean equals(Object obj) {
        // sessions are equal if the ids are the same, allows comparison across reloaded items
        if (null == obj) {
            return false;
        }
        if (!(obj instanceof SessionImpl)) {
            return false;
        } else {
            SessionImpl castObj = (SessionImpl) obj;
            boolean eq;
            try {
                eq = this.getId().equals(castObj.getId());
            } catch (IllegalStateException e) {
                eq = false;
            }
            return eq;
        }
    }

    @Override
    public int hashCode() {
        String hashStr = this.getClass().getName() + ":" + this.httpSession.toString();
        return hashStr.hashCode();
    }

    @Override
    public String toString() {
        String str;
        if (isInvalidated()) {
            str = "invalidated:" + this.httpSession.toString() + ":" + super.toString();
        } else {
            str = "active:"+getId()+":user="+getUserId()+"("+getUserEID()+"):sid="+getSessionId()+":server="+getServerId()+":created="+getCreationTime()+":accessed="+getLastAccessedTime()+":maxInactiveSecs="+getMaxInactiveInterval()+":hostIP="+getOriginatingHostIP()+":hostName="+getOriginatingHostName()+":"+super.toString();
        }
        return "Session:" + str;
    }


    // INTERFACE methods

    /* (non-Javadoc)
     * @see org.dspace.services.model.Session#getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String key) {
        return getKeyAttribute(key);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Session#setAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public void setAttribute(String key, String value) {
        setKeyAttribute(key, value);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name) {
        removeKeyAttribute(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (value != null && !(value instanceof String)) {
            throw new UnsupportedOperationException("Invalid session attribute ("+name+","+value+"), Only strings can be stored in the session");
        }
        setKeyAttribute(name, (String) value);
    }

    /**
     * @return a copy of the attributes in this session.
     * Modifying it has no effect on the session attributes.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> map = new HashMap<String, String>();
        if (! isInvalidated()) {
            Enumeration<String> names = this.httpSession.getAttributeNames();
            while (names.hasMoreElements()) {
                String key = names.nextElement();
                String value = (String) this.httpSession.getAttribute(key);
                map.put(key, value);
            }
        }
        return map;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getAttributeNames() {
        return this.httpSession.getAttributeNames();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.Session#clear()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void clear() {
        if (! isInvalidated()) {
            Enumeration<String> names = this.httpSession.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                this.httpSession.removeAttribute(name);
            }
        }
    }

    @Override
    public String getOriginatingHostIP() {
        return getKeyAttribute(HOST_IP);
    }

    @Override
    public String getOriginatingHostName() {
        return getKeyAttribute(HOST_NAME);
    }

    @Override
    public String getServerId() {
        return getKeyAttribute(SERVER_ID);
    }

    @Override
    public String getSessionId() {
        return getKeyAttribute(SESSION_ID);
    }

    @Override
    public String getUserEID() {
        return getKeyAttribute(USER_EID);
    }

    @Override
    public String getUserId() {
        return getKeyAttribute(USER_ID);
    }

    @Override
    public boolean isActive() {
        return ! isInvalidated();
    }

    // HTTP SESSION passthroughs

    @Override
    public long getCreationTime() {
        return this.httpSession.getCreationTime();
    }

    @Override
    public String getId() {
        String id = null;
        if (isAttributeSet(SESSION_ID)) {
            id = getKeyAttribute(SESSION_ID);
        } else {
            id = this.httpSession.getId();
        }
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return this.httpSession.getLastAccessedTime();
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.httpSession.getMaxInactiveInterval();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.httpSession.setMaxInactiveInterval(interval);
    }

    @Override
    public ServletContext getServletContext() {
        if (this.httpSession != null) {
            return this.httpSession.getServletContext();
        }
        throw new UnsupportedOperationException("No http session available for this operation");
    }

    @Override
    public void invalidate() {
        if (! isInvalidated()) {
            this.httpSession.invalidate();
        }
        // TODO nothing otherwise?
    }

    @Override
    public boolean isNew() {
        if (! isInvalidated()) {
            return this.httpSession.isNew();
        }
        return false;
    }

    // DEPRECATED

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     */
    @Override
    public Object getValue(String name) {
        return getKeyAttribute(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getValueNames()
     */
    @Override
    public String[] getValueNames() {
        Set<String> keys = getAttributes().keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     */
    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     */
    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public HttpSessionContext getSessionContext() {
        if (this.httpSession != null) {
            return this.httpSession.getSessionContext();
        }
        throw new UnsupportedOperationException("No http session available for this operation");
    }

    // END DEPRECATED



    /**
     * Check if something is blank (null or "").
     *
     * @param string string to check
     * @return true if is blank
     */
    public static boolean isBlank(String string) {
        return (string == null) || ("".equals(string));
    }

    /**
     * Compares sessions by the last time they were accessed, with more 
     * recent first.
     */
    public static final class SessionLastAccessedComparator implements Comparator<Session>, Serializable {
        public static final long serialVersionUID = 1l;
        public int compare(Session o1, Session o2) {
            try {
                Long lat1 = Long.valueOf(o1.getLastAccessedTime());
                Long lat2 = Long.valueOf(o2.getLastAccessedTime());
                return lat2.compareTo(lat1); // reverse
            } catch (Exception e) {
                return 0;
            }
        }
    }

}
