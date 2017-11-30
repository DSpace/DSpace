/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import org.dspace.services.model.Session;

/**
 * Provides access to user sessions and allows for initializing user 
 * sessions.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface SessionService {

    /**
     * Start a new session and destroy any existing session that is 
     * known for this thread.
     * Will bind this to the current request and capture all required 
     * session information
     * <p>
     * WARNING: there is normally no need to call this method as the 
     * session is created for you when using webapps.  This is only
     * needed when there is a requirement to create a session operating
     * outside a servlet container or manually handling sessions.
     * 
     * @return the Session object associated with the current request or processing thread OR null if there is not one
     */
    public Session getCurrentSession();

    /**
     * Access the current session id for the current thread
     * (also available from the current session).
     * 
     * @return the id of the session associated with the current thread OR null if there is no session
     */
    public String getCurrentSessionId();

    /**
     * Access the current user id for the current session.
     * (also available from the current session)
     * 
     * @return the id of the user associated with the current thread OR null if there is no user
     */
    public String getCurrentUserId();

}
