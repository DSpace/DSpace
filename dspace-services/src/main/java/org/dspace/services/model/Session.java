/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;

import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 * Represents a user's session (login session) in the system.  Can hold 
 * some additional attributes as needed, but the underlying
 * implementation may limit the number and size of attributes to ensure
 * session replication is not impacted negatively.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface Session extends HttpSession {

    /**
     * @return the session identifier.  This is not the {@link #getId()} 
     * from HttpSession unless no session id was specified when the 
     * session was bound.
     */
    public String getSessionId();

    /**
     * Return the internal user ID for this session.
     *
     * @return internal user ID for the user using this session.
     * This is null if the session is anonymous.
     */
    public String getUserId();

    /**
     * Get the external/enterprise user ID for this session.
     *
     * @return the external/enterprise user id of the user associated with this session
     */
    public String getUserEID();

    /**
     * @return true if this session is active OR false if the session has timed out or been invalidated
     */
    public boolean isActive();

    /**
     * @return id of the server with which this session is associated.
     */
    public String getServerId();

    /**
     * @return the IP Address from which this session originated 
     */
    public String getOriginatingHostIP();

    /**
     * @return the hostname from which this session originated
     */
    public String getOriginatingHostName();

    /**
     * Get an attribute from the session if one exists.
     * 
     * @param key
     *            the key for the attribute
     * @return the value if one exists OR null if none
     */
    public String getAttribute(String key);

    /**
     * Set an attribute on a session.
     * 
     * @param key
     *            the key for the attribute
     * @param value
     *            the value (if this is null then the attribute is removed)
     */
    public void setAttribute(String key, String value);

    /**
     * Get all attributes of this session.
     *
     * @return a copy of the attributes in this session.
     * Modifying it has no effect on the session attributes.
     */
    public Map<String, String> getAttributes();

    /**
     * Purges all data from this session and effectively resets it to an 
     * anonymous session.  Does not invalidate the session, though.
     */
    public void clear();

}
