/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.services;

import java.util.List;

import org.dspace.services.model.Session;

/**
 * Provides access to user sessions and allows for initializing user sessions
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface SessionService {

    /**
     * Start a new session and destroy any existing session that is known for this thread,
     * will bind this to the current request and capture all required session information <br/>
     * WARNING: there is normally no need to call this method as the session is created
     * for you when using webapps, this is only needed when there is a requirement to
     * create a session (operating outside a servlet container or manually handling sessions)
     * 
     * @param sessionId (optional) if null this is generated automatically, 
     * otherwise the given session ID will be used if it is not already taken or assigned
     * @return the session object
     */
    public Session startSession(String sessionId);

    /**
     * Bind the session for the given user (or anonymous),
     * this is useful for associating an authenticated user with a session <br/>
     * 
     * @param sessionId the unique ID for a session
     * @param userId (optional) the internal user ID (not the username),
     * can set this to null for an anonymous user or to remove the user binding from a session
     * @param userEid (optional) the external user ID (typically the username),
     * ignored if userId is null, must be set if userId is set
     * @return the session with the given id
     * @throws IllegalArgumentException if the sessionId is null or the session with that id cannot be found OR
     * the userId is set and userEid is not set
     */
    public Session bindSession(String sessionId, String userId, String userEid);

    /**
     * Retrieves a session by the sessionId if it is active <br/>
     * WARNING: there is normally no need to call this method, use {@link #getCurrentSession()}
     * 
     * @param sessionId the unique id for a session (not {@link Session#getId()}, this is {@link Session#getSessionId()})
     * @return a session if one is available OR null if none found
     * @throws IllegalArgumentException if the sessionId is null
     */
    public Session getSession(String sessionId);

    /**
     * Get the list of sessions,
     * this will automatically purge out any sessions which have expired
     * @return the list of all active sessions ordered by last time accessed
     */
    public List<Session> getAllActiveSessions();

    /**
     * Access the current session for the current thread,
     * this contains information about the current user as well
     * 
     * @return the Session object associated with the current request or processing thread OR null if there is not one
     */
    public Session getCurrentSession();

    /**
     * Access the current session id for the current thread
     * (also available from the current session)
     * 
     * @return the id of the session associated with the current thread OR null if there is no session
     */
    public String getCurrentSessionId();

    /**
     * Access the current user id for the current session
     * (also available from the current session)
     * 
     * @return the id of the user associated with the current thread OR null if there is no user
     */
    public String getCurrentUserId();

}
