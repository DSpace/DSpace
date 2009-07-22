/**
 * $Id: Session.java 3246 2008-10-28 18:02:05Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/model/Session.java $
 * Session.java - DSpace2 - Oct 14, 2008 11:46:22 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services.model;

import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 * Represents a users session (login session) in the system, can hold some additional attributes as
 * needed but the underlying implementation may limit the number and size of attributes to ensure
 * session replication is not impacted negatively
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface Session extends HttpSession {

    /**
     * @return the session identifier, this is not the {@link #getId()} from HttpSession
     * unless no session id was specified when the session was bound
     */
    public String getSessionId();

    /**
     * @return internal user ID for the user using this session,
     * this is null if the session is anonymous
     */
    public String getUserId();

    /**
     * @return the external/enterprise user id of the user associated with this session
     */
    public String getUserEID();

    /**
     * @return true if this session is active OR false if the session has timed out or been invalidated
     */
    public boolean isActive();

    /**
     * @return id of the server which this session is associated with
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
     * Get an attribute from the session if one exists
     * 
     * @param key
     *            the key for the attribute
     * @return the value if one exists OR null if none
     */
    public String getAttribute(String key);

    /**
     * Set an attribute on a session
     * 
     * @param key
     *            the key for the attribute
     * @param value
     *            the value (if this is null then the attribute is removed)
     */
    public void setAttribute(String key, String value);

    /**
     * @return a copy of the attributes in this session, 
     * modifying it has no effect on the session attributes
     */
    public Map<String, String> getAttributes();

    /**
     * Purges all data from this session and effectively resets it to an anonymous session,
     * does not invalidate the session though
     */
    public void clear();

}
