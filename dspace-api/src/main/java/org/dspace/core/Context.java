/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Dispatcher;
import org.dspace.event.Event;
import org.dspace.event.EventManager;
import org.dspace.services.CachingService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.util.XMLBindUtils;
import org.springframework.beans.factory.annotation.Autowired;

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
 * @author DSpace @ Lyncode <dspace@lyncode.com> - Contribution: Using Caching Service instead of old Map
 */
public class Context
{
    private static final Logger log = Logger.getLogger(Context.class);
    private static final String CACHING_CONFIGURATION = "org/dspace/services/caching/ehcache-config.xml";

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

    /** A stack with the history of authorisation system check modify */
    private Stack<Boolean> authStateChangeHistory;

    /**
     * A stack with the name of the caller class that modify authorisation
     * system check
     */
    private Stack<String> authStateClassCallHistory;
    
    /**
     * Caching service
     */
    @Autowired
	private CachingService cacheService;
    private Cache cache;

    /** Group IDs of special groups user is a member of */
    private List<Integer> specialGroups;

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

        specialGroups = new ArrayList<Integer>();

        authStateChangeHistory = new Stack<Boolean>();
        authStateClassCallHistory = new Stack<String>();
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
     * Gets the current Locale
     * 
     * @return Locale the current Locale
     */
    public Locale getCurrentLocale()
    {
        return currentLocale;
    }

    /**
     * set the current Locale
     * 
     * @param locale
     *            the current Locale
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
     * Turn Off the Authorisation System for this context and store this change
     * in a history for future use.
     */
    public void turnOffAuthorisationSystem()
    {
        authStateChangeHistory.push(ignoreAuth);
        if (log.isDebugEnabled())
        {
            Thread currThread = Thread.currentThread();
            StackTraceElement[] stackTrace = currThread.getStackTrace();
            String caller = stackTrace[stackTrace.length - 1].getClassName();

            authStateClassCallHistory.push(caller);
        }
        ignoreAuth = true;
    }

    /**
     * Restore the previous Authorisation System State. If the state was not
     * changed by the current caller a warning will be displayed in log. Use:
     * <code>
     *     mycontext.turnOffAuthorisationSystem();
     *     some java code that require no authorisation check
     *     mycontext.restoreAuthSystemState(); 
         * </code> If Context debug is enabled, the correct sequence calling will be
     * checked and a warning will be displayed if not.
     */
    public void restoreAuthSystemState()
    {
        Boolean previousState;
        try
        {
            previousState = authStateChangeHistory.pop();
        }
        catch (EmptyStackException ex)
        {
            log.warn(LogManager.getHeader(this, "restore_auth_sys_state",
                    "not previous state info available "
                            + ex.getLocalizedMessage()));
            previousState = Boolean.FALSE;
        }
        if (log.isDebugEnabled())
        {
            Thread currThread = Thread.currentThread();
            StackTraceElement[] stackTrace = currThread.getStackTrace();
            String caller = stackTrace[stackTrace.length - 1].getClassName();

            String previousCaller = (String) authStateClassCallHistory.pop();

            // if previousCaller is not the current caller *only* log a warning
            if (!previousCaller.equals(caller))
            {
                log
                        .warn(LogManager
                                .getHeader(
                                        this,
                                        "restore_auth_sys_state",
                                        "Class: "
                                                + caller
                                                + " call restore but previous state change made by "
                                                + previousCaller));
            }
        }
        ignoreAuth = previousState.booleanValue();
    }

    /**
     * Specify whether the authorisation system should be ignored for this
     * context. This should be used sparingly.
     * 
     * @deprecated use turnOffAuthorisationSystem() for make the change and
     *             restoreAuthSystemState() when change are not more required
     * @param b
     *            if <code>true</code>, authorisation should be ignored for this
     *            session.
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
     * context have completed successfully. Any transaction with the database is
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
            clearCache();
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
    public void commit() throws SQLException
    {
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
            if (dispatcher != null)
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
            log.debug(this.toString() + ": setDispatcher(\"" + dispatcher
                    + "\")");
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
     * TODO WARNING: events uses an ArrayList, a class not ready for concurrency.
     * Read http://download.oracle.com/javase/6/docs/api/java/util/Collections.html#synchronizedList%28java.util.List%29
     * on how to properly synchronize the class when calling this method
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
            if (!connection.isClosed())
            {
                connection.rollback();
            }
        }
        catch (SQLException se)
        {
            log.error(se.getMessage(), se);
        }
        finally
        {
            try
            {
                if (!connection.isClosed())
                {
                    DatabaseManager.freeConnection(connection);
                }
            }
            catch (Exception ex)
            {
                log.error("Exception aborting context", ex);
            }
            connection = null;
            events = null;
            clearCache();
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
    public Object fromCache(Class<?> objectClass, int id)
    {
        String key = objectClass.getName() + id;
        return this.getCache().get(key);
    }
    
    private Cache getCache () {
    	if (this.cache == null) {
    		CacheConfig config;
			try {
				config = (CacheConfig) XMLBindUtils.unmarshall(this.getClass().getClassLoader().getResourceAsStream(CACHING_CONFIGURATION), CacheConfig.class);
			} catch (JAXBException e) {
				// FIXME: Maybe it would be fair to not fail...
				throw new IllegalStateException("Failed to read cache configuration.", e);
			}
    		this.cache = this.getCacheService().getCache(this.getClass().getName(), config);
    	}
    	return this.cache;
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
        this.getCache().put(key, o);
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
        this.getCache().remove(key);
    }

    /**
     * Remove all the objects from the object cache
     */
    public void clearCache()
    {
    	this.getCache().clear();
    }

    /**
     * Get the count of cached objects, which you can use to instrument an
     * application to track whether it is "leaking" heap space by letting cached
     * objects build up. We recommend logging a cache count periodically or
     * episodically at the INFO or DEBUG level, but ONLY when you are diagnosing
     * cache leaks.
     * 
     * @return count of entries in the cache.
     * 
     * @return the number of items in the cache
     */
    public int getCacheSize()
    {
    	return this.getCache().size();
    }

    /**
     * set membership in a special group
     * 
     * @param groupID
     *            special group's ID
     */
    public void setSpecialGroup(int groupID)
    {
        specialGroups.add(Integer.valueOf(groupID));

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
        if (specialGroups.contains(Integer.valueOf(groupID)))
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
        List<Group> myGroups = new ArrayList<Group>();
        for (Integer groupId : specialGroups)
        {
            myGroups.add(Group.find(this, groupId.intValue()));
        }

        return myGroups.toArray(new Group[myGroups.size()]);
    }

    protected void finalize() throws Throwable
    {
        /*
         * If a context is garbage-collected, we roll back and free up the
         * database connection if there is one.
         */
        if (connection != null)
        {
            abort();
        }

        super.finalize();
    }

	public CachingService getCacheService() {
		return cacheService;
	}
	
	public void setChacheService (CachingService cachingService) {
		cacheService = cachingService;
	}
}
