/*
 * Context.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.event.EventManager;
import org.dspace.event.Dispatcher;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * Class representing the context of a particular DSpace operation. This stores
 * information such as the current authenticated user and the database
 * connection being used.
 * <P>
 * Typical use of the context object will involve constructing one, and setting
 * the current user if one is authenticated. Several operations may be performed
 * using the context object. If all goes well, <code>complete</code> is called
 * to commit the changes and free up any resources used by the context. If
 * anything has gone wrong, <code>abort</code> is called to roll back any
 * changes and free up the resources.
 * <P>
 * The context object is also used as a cache for CM API objects.
 * 
 * 
 * @version $Revision$
 */
public class Context
{
    private static final Logger log = Logger.getLogger(Context.class);

    /** Database connection */
    private Connection connection;

    /** Current user - null means anonymous access */
    private EPerson currentUser;
    
    /** Current Locale */
    private Locale currentLocale;

    /** Extra log info */
    private String extraLogInfo;

    /** Indicates whether authorisation subsystem should be ignored */
    private boolean ignoreAuth;

    /** Object cache for this context */
    private Map objectCache;

    /** Group IDs of special groups user is a member of */
    private List specialGroups;
    
    /** Content events */
    private List<Event> events = null;

    /** Event dispatcher name */
    private String dispName = null;

    /**
     * Construct a new context object. A database connection is opened. No user
     * is authenticated.
     * 
     * @exception SQLException
     *                if there was an error obtaining a database connection
     */
    public Context() throws SQLException
    {
        // Obtain a non-auto-committing connection
        connection = DatabaseManager.getConnection();
        connection.setAutoCommit(false);

        currentUser = null;
        currentLocale = I18nUtil.DEFAULTLOCALE;
        extraLogInfo = "";
        ignoreAuth = false;

        objectCache = new HashMap();
        specialGroups = new ArrayList();
    }

    /**
     * Get the database connection associated with the context
     * 
     * @return the database connection
     */
    public Connection getDBConnection()
    {
        return connection;
    }

    /**
     * Set the current user. Authentication must have been performed by the
     * caller - this call does not attempt any authentication.
     * 
     * @param user
     *            the new current user, or <code>null</code> if no user is
     *            authenticated
     */
    public void setCurrentUser(EPerson user)
    {
        currentUser = user;
    }

    /**
     * Get the current (authenticated) user
     * 
     * @return the current user, or <code>null</code> if no user is
     *         authenticated
     */
    public EPerson getCurrentUser()
    {
        return currentUser;
    }

    /**
     *  Gets the current Locale
     *  
     *  @return Locale
     *          the current Locale
     */
    public Locale getCurrentLocale()
    {
        return currentLocale;
    }
 
    /**
     *  set the current Locale
     *  
     *  @param Locale
     *          the current Locale
     */
    public void setCurrentLocale(Locale locale)
    {
        currentLocale = locale;
    }
        
    /**
     * Find out if the authorisation system should be ignored for this context.
     * 
     * @return <code>true</code> if authorisation should be ignored for this
     *         session.
     */
    public boolean ignoreAuthorization()
    {
        return ignoreAuth;
    }

    /**
     * Specify whether the authorisation system should be ignored for this
     * context. This should be used sparingly.
     * 
     * @param b
     *            if <code>true</code>, authorisation should be ignored for
     *            this session.
     */
    public void setIgnoreAuthorization(boolean b)
    {
        ignoreAuth = b;
    }

    /**
     * Set extra information that should be added to any message logged in the
     * scope of this context. An example of this might be the session ID of the
     * current Web user's session:
     * <P>
     * <code>setExtraLogInfo("session_id="+request.getSession().getId());</code>
     * 
     * @param info
     *            the extra information to log
     */
    public void setExtraLogInfo(String info)
    {
        extraLogInfo = info;
    }

    /**
     * Get extra information to be logged with message logged in the scope of
     * this context.
     * 
     * @return the extra log info - guaranteed non- <code>null</code>
     */
    public String getExtraLogInfo()
    {
        return extraLogInfo;
    }
       

    /**
     * Close the context object after all of the operations performed in the
     * context have completed succesfully. Any transaction with the database is
     * committed.
     * 
     * @exception SQLException
     *                if there was an error completing the database transaction
     *                or closing the connection
     */
    public void complete() throws SQLException
    {
        // FIXME: Might be good not to do a commit() if nothing has actually
        // been written using this connection
        try
        {
            // Commit any changes made as part of the transaction
            commit();
        }
        finally
        {
            // Free the connection
            DatabaseManager.freeConnection(connection);
            connection = null;
        }
    }

    /**
     * Commit any transaction that is currently in progress, but do not close
     * the context.
     * 
     * @exception SQLException
     *                if there was an error completing the database transaction
     *                or closing the connection
     */
    public void commit() throws SQLException {
        // Commit any changes made as part of the transaction
        Dispatcher dispatcher = null;

        try
        {
            if (events != null)
            {

                if (dispName == null)
                {
                    dispName = EventManager.DEFAULT_DISPATCHER;
                }

                dispatcher = EventManager.getDispatcher(dispName);
                connection.commit();
                dispatcher.dispatch(this);
            }
            else
            {
                connection.commit();
            }

        }
        finally
        {
            events = null;
            if(dispatcher != null) 
            {
                EventManager.returnDispatcher(dispName, dispatcher);
            }
        }

    }

    /**
     * Select an event dispatcher, <code>null</code> selects the default
     * 
     */
    public void setDispatcher(String dispatcher)
    {
        if (log.isDebugEnabled())
        {
            log.debug(this.toString() + ": setDispatcher(\"" + dispatcher + "\")");
        }
        dispName = dispatcher;
    }

    /**
     * Add an event to be dispatched when this context is committed.
     * 
     * @param event
     */
    public void addEvent(Event event)
    {
        if (events == null)
        {
            events = new ArrayList<Event>();
        }
        
        events.add(event);
    }

    /**
     * Get the current event list. If there is a separate list of events from
     * already-committed operations combine that with current list.
     * 
     * @return List of all available events.
     */
    public List<Event> getEvents()
    {
        return events;
    }

    
    /**
     * Close the context, without committing any of the changes performed using
     * this context. The database connection is freed. No exception is thrown if
     * there is an error freeing the database connection, since this method may
     * be called as part of an error-handling routine where an SQLException has
     * already been thrown.
     */
    public void abort()
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException se)
        {
            log.error(se.getMessage());
            se.printStackTrace();
        }
        finally
        {
            DatabaseManager.freeConnection(connection);
            connection = null;
            events = null;
        }
    }

    /**
     * 
     * Find out if this context is valid. Returns <code>false</code> if this
     * context has been aborted or completed.
     * 
     * @return <code>true</code> if the context is still valid, otherwise
     *         <code>false</code>
     */
    public boolean isValid()
    {
        // Only return true if our DB connection is live
        return (connection != null);
    }

    /**
     * Store an object in the object cache.
     * 
     * @param objectClass
     *            Java Class of object to check for in cache
     * @param id
     *            ID of object in cache
     * 
     * @return the object from the cache, or <code>null</code> if it's not
     *         cached.
     */
    public Object fromCache(Class objectClass, int id)
    {
        String key = objectClass.getName() + id;

        return objectCache.get(key);
    }

    /**
     * Store an object in the object cache.
     * 
     * @param o
     *            the object to store
     * @param id
     *            the object's ID
     */
    public void cache(Object o, int id)
    {
        String key = o.getClass().getName() + id;
        objectCache.put(key, o);
    }

    /**
     * Remove an object from the object cache.
     * 
     * @param o
     *            the object to remove
     * @param id
     *            the object's ID
     */
    public void removeCached(Object o, int id)
    {
        String key = o.getClass().getName() + id;
        objectCache.remove(key);
    }

    /**
     * Remove all the objects from the object cache
     */
    public void clearCache()
    {
    	objectCache.clear();
    }
    
    /**
     * set membership in a special group
     * 
     * @param groupID
     *            special group's ID
     */
    public void setSpecialGroup(int groupID)
    {
        specialGroups.add(new Integer(groupID));

        // System.out.println("Added " + groupID);
    }

    /**
     * test if member of special group
     * 
     * @param groupID
     *            ID of special group to test
     * @return true if member
     */
    public boolean inSpecialGroup(int groupID)
    {
        if (specialGroups.contains(new Integer(groupID)))
        {
            // System.out.println("Contains " + groupID);
            return true;
        }

        return false;
    }

    /**
     * gets an array of all of the special groups that current user is a member
     * of
     * 
     * @return
     * @throws SQLException
     */
    public Group[] getSpecialGroups() throws SQLException
    {
        List myGroups = new ArrayList();

        Iterator i = specialGroups.iterator();

        while (i.hasNext())
        {
            myGroups.add(Group.find(this, ((Integer) i.next()).intValue()));
        }

        return (Group[]) myGroups.toArray(new Group[0]);
    }

    protected void finalize()
    {
        /*
         * If a context is garbage-collected, we roll back and free up the
         * database connection if there is one.
         */
        if (connection != null)
        {
            abort();
        }
    }
}
