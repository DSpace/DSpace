/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.apache.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.exception.DatabaseSchemaValidationException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.event.Dispatcher;
import org.dspace.event.Event;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.storage.rdbms.DatabaseConfigVO;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.utils.DSpace;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.*;

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

    /** Group IDs of special groups user is a member of */
    private List<UUID> specialGroups;

    /** Content events */
    private LinkedList<Event> events = null;

    /** Event dispatcher name */
    private String dispName = null;

    /** Context mode */
    private Mode mode = Mode.READ_WRITE;

    /** Cache that is only used the context is in READ_ONLY mode */
    private ContextReadOnlyCache readOnlyCache = new ContextReadOnlyCache();

    protected EventService eventService;

    private DBConnection dbConnection;

    public enum Mode {
        READ_ONLY,
        READ_WRITE,
        BATCH_EDIT
    }

    static
    {
        // Before initializing a Context object, we need to ensure the database
        // is up-to-date. This ensures any outstanding Flyway migrations are run
        // PRIOR to Hibernate initializing (occurs when DBConnection is loaded in init() below).
        try
        {
            DatabaseUtils.updateDatabase();
        }
        catch(SQLException sqle)
        {
            log.fatal("Cannot initialize database via Flyway!", sqle);
        }
    }

    protected Context(EventService eventService, DBConnection dbConnection)  {
        this.mode = Mode.READ_WRITE;
        this.eventService = eventService;
        this.dbConnection = dbConnection;
        init();
    }


    /**
     * Construct a new context object with default options. A database connection is opened.
     * No user is authenticated.
     */
    public Context()
    {
        this.mode = Mode.READ_WRITE;
        init();
    }

    /**
     * Construct a new context object with the given mode enabled. A database connection is opened.
     * No user is authenticated.
     * 
     * @param mode   The mode to use when opening the context.
     */
    public Context(Mode mode)
    {
        this.mode = mode;
        init();
    }

    /**
     * Initializes a new context object. 
     *
     * @exception SQLException
     *                if there was an error obtaining a database connection
     */
    private void init()
    {
        if(eventService == null)
        {
            eventService = EventServiceFactory.getInstance().getEventService();
        }
        if(dbConnection == null)
        {
            // Obtain a non-auto-committing connection
            dbConnection = new DSpace().getSingletonService(DBConnection.class);
            if(dbConnection == null)
            {
                //It appears there is a problem with the database, run the Schema validator
                DatabaseSchemaValidator schemaValidator = new DSpace().getSingletonService(DatabaseSchemaValidator.class);

                String validationError = schemaValidator == null ? "null" : schemaValidator.getDatabaseSchemaValidationError();
                String message = "The schema validator returned: " +
                        validationError;

                log.fatal("Cannot obtain the bean which provides a database connection. " +
                        "Check previous entries in the dspace.log to find why the db failed to initialize. " + message);

                //Fail fast
                throw new DatabaseSchemaValidationException(message);
            }
        }

        currentUser = null;
        currentLocale = I18nUtil.DEFAULTLOCALE;
        extraLogInfo = "";
        ignoreAuth = false;

        specialGroups = new ArrayList<>();

        authStateChangeHistory = new Stack<Boolean>();
        authStateClassCallHistory = new Stack<String>();
        setMode(this.mode);
    }

    /**
     * Get the database connection associated with the context
     * 
     * @return the database connection
     */
    DBConnection getDBConnection()
    {
        return dbConnection;
    }

    public DatabaseConfigVO getDBConfig() throws SQLException
    {
        return dbConnection.getDatabaseConfig();
    }

    public String getDbType(){
        return dbConnection.getType();
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
     * <p>
     * Calling complete() on a Context which is no longer valid (isValid()==false),
     * is a no-op.
     * 
     * @exception SQLException
     *                if there was an error completing the database transaction
     *                or closing the connection
     */
    public void complete() throws SQLException
    {
        // If Context is no longer open/valid, just note that it has already been closed
        if(!isValid())
            log.info("complete() was called on a closed Context object. No changes to commit.");

        try
        {
            // As long as we have a valid, writeable database connection,
            // rollback any changes if we are in read-only mode,
            // otherwise, commit any changes made as part of the transaction
            if(isReadOnly()) {
                abort();
            } else {
                commit();
            }
        }
        finally
        {
            if(dbConnection != null)
            {
                // Free the DB connection
                dbConnection.closeDBConnection();
                dbConnection = null;
            }
        }
    }

    /**
     * Commit the current transaction with the database, persisting any pending changes.
     * The database connection is not closed and can be reused afterwards.
     *
     * <b>WARNING: After calling this method all previously fetched entities are "detached" (pending
     * changes are not tracked anymore). You have to reload all entities you still want to work with
     * manually after this method call (see {@link Context#reloadEntity(ReloadableEntity)}).</b>
     *
     * @throws SQLException When committing the transaction in the database fails.
     */
    public void commit() throws SQLException
    {
        // If Context is no longer open/valid, just note that it has already been closed
        if(!isValid()) {
            log.info("commit() was called on a closed Context object. No changes to commit.");
        }

        if(isReadOnly()) {
            throw new UnsupportedOperationException("You cannot commit a read-only context");
        }

        // Our DB Connection (Hibernate) will decide if an actual commit is required or not
        try
        {
            // As long as we have a valid, writeable database connection,
            // commit any changes made as part of the transaction
            if (isValid())
            {
                // Dispatch events before committing changes to the database,
                // as the consumers may change something too
                dispatchEvents();
            }

        } finally {
            if(log.isDebugEnabled()) {
                log.debug("Cache size on commit is " + getCacheSize());
            }

            if(dbConnection != null)
            {
                //Commit our changes
                dbConnection.commit();
                reloadContextBoundEntities();
            }
        }
    }


    public void dispatchEvents()
    {
        // Commit any changes made as part of the transaction
        Dispatcher dispatcher = null;

        try {
            if (events != null) {

                if (dispName == null) {
                    dispName = EventService.DEFAULT_DISPATCHER;
                }

                dispatcher = eventService.getDispatcher(dispName);
                dispatcher.dispatch(this);
            }
        } finally {
            events = null;
            if (dispatcher != null) {
                eventService.returnDispatcher(dispName, dispatcher);
            }
        }
    }

    /**
     * Select an event dispatcher, <code>null</code> selects the default
     * 
     * @param dispatcher dispatcher
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
        /* 
         * invalid condition if in read-only mode: events - which
         * indicate mutation - are firing: no recourse but to bail
         */
        if (isReadOnly())
        {
            throw new IllegalStateException("Attempt to mutate object in read-only context");
        }
        if (events == null)
        {
            events = new LinkedList<Event>();
        }

        events.add(event);
    }

    /**
     * Get the current event list. If there is a separate list of events from
     * already-committed operations combine that with current list.
     * 
     * @return List of all available events.
     */
    public LinkedList<Event> getEvents()
    {
        return events;
    }

    public boolean hasEvents()
    {
        return !CollectionUtils.isEmpty(events);
    }

    /**
     * Retrieves the first element in the events list and removes it from the list of events once retrieved
     * @return The first event of the list or <code>null</code> if the list is empty
     */
    public Event pollEvent()
    {
        if(hasEvents())
        {
            return events.poll();
        }else{
            return null;
        }
    }

    /**
     * Close the context, without committing any of the changes performed using
     * this context. The database connection is freed. No exception is thrown if
     * there is an error freeing the database connection, since this method may
     * be called as part of an error-handling routine where an SQLException has
     * already been thrown.
     * <p>
     * Calling abort() on a Context which is no longer valid (isValid()==false),
     * is a no-op.
     */
    public void abort()
    {
        // If Context is no longer open/valid, just note that it has already been closed
        if(!isValid())
            log.info("abort() was called on a closed Context object. No changes to abort.");

        try
        {
            // Rollback ONLY if we have a database connection, and it is NOT Read Only
            if (isValid() && !isReadOnly())
            {
                dbConnection.rollback();
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
                if (!dbConnection.isSessionAlive())
                {
                    dbConnection.closeDBConnection();
                }
            }
            catch (Exception ex)
            {
                log.error("Exception aborting context", ex);
            }
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
        return dbConnection != null && dbConnection.isTransActionAlive();
    }

    /**
     * Reports whether context supports updating DSpaceObjects, or only reading.
     * 
     * @return <code>true</code> if the context is read-only, otherwise
     *         <code>false</code>
     */
    public boolean isReadOnly()
    {
        return mode != null && mode == Mode.READ_ONLY;
    }

    public void setSpecialGroup(UUID groupID)
    {
        specialGroups.add(groupID);

        // System.out.println("Added " + groupID);
    }

    /**
     * test if member of special group
     * 
     * @param groupID
     *            ID of special group to test
     * @return true if member
     */
    public boolean inSpecialGroup(UUID groupID)
    {
        if (specialGroups.contains(groupID))
        {
            // System.out.println("Contains " + groupID);
            return true;
        }

        return false;
    }

    /**
     * Get an array of all of the special groups that current user is a member
     * of.
     * @return list of groups
     * @throws SQLException if database error
     */
    public List<Group> getSpecialGroups() throws SQLException
    {
        List<Group> myGroups = new ArrayList<Group>();
        for (UUID groupId : specialGroups)
        {
            myGroups.add(EPersonServiceFactory.getInstance().getGroupService().find(this, groupId));
        }

        return myGroups;
    }

    @Override
    protected void finalize() throws Throwable
    {
        /*
         * If a context is garbage-collected, we roll back and free up the
         * database connection if there is one.
         */
        if (dbConnection != null && dbConnection.isTransActionAlive())
        {
            abort();
        }

        super.finalize();
    }

    public void shutDownDatabase() throws SQLException {
        dbConnection.shutdown();
    }


    /**
     * Returns the size of the cache of all object that have been read from the database so far. A larger number
     * means that more memory is consumed by the cache. This also has a negative impact on the query performance. In
     * that case you should consider uncaching entities when they are no longer needed (see {@link Context#uncacheEntity(ReloadableEntity)} () uncacheEntity}).
     *
     * @throws SQLException When connecting to the active cache fails.
     */
    public long getCacheSize() throws SQLException {
        return this.getDBConnection().getCacheSize();
    }

    /**
     * Change the mode of this current context.
     *
     * BATCH_EDIT: Enabling batch edit mode means that the database connection is configured so that it is optimized to
     * process a large number of records.
     *
     * READ_ONLY: READ ONLY mode will tell the database we are nog going to do any updates. This means it can disable
     * optimalisations for delaying or grouping updates.
     *
     * READ_WRITE: This is the default mode and enables the normal database behaviour. This behaviour is optimal for querying and updating a
     * small number of records.
     *
     * @param newMode The mode to put this context in
     */
    public void setMode(Mode newMode) {
        try {
            //update the database settings
            switch (newMode) {
                case BATCH_EDIT:
                    dbConnection.setConnectionMode(true, false);
                    break;
                case READ_ONLY:
                    dbConnection.setConnectionMode(false, true);
                    break;
                case READ_WRITE:
                    dbConnection.setConnectionMode(false, false);
                    break;
                default:
                    log.warn("New context mode detected that has nog been configured.");
                    break;
            }
        } catch(SQLException ex) {
            log.warn("Unable to set database connection mode", ex);
        }

        //Always clear the cache, except when going from READ_ONLY to READ_ONLY
        if(mode != Mode.READ_ONLY || newMode != Mode.READ_ONLY) {
            //clear our read-only cache to prevent any inconsistencies
            readOnlyCache.clear();
        }

        //save the new mode
        mode = newMode;
    }

    /**
     * The current database mode of this context.
     * @return The current mode
     */
    public Mode getCurrentMode() {
        return mode;
    }

    /**
     * Enable or disable "batch processing mode" for this context.
     *
     * Enabling batch processing mode means that the database connection is configured so that it is optimized to
     * process a large number of records.
     *
     * Disabling batch processing mode restores the normal behaviour that is optimal for querying and updating a
     * small number of records.
     *
     * @param batchModeEnabled When true, batch processing mode will be enabled. If false, it will be disabled.
     * @throws SQLException When configuring the database connection fails.
     */
    @Deprecated
    public void enableBatchMode(boolean batchModeEnabled) throws SQLException {
        if(batchModeEnabled) {
            setMode(Mode.BATCH_EDIT);
        } else {
            setMode(Mode.READ_WRITE);
        }
    }

    /**
     * Check if "batch processing mode" is enabled for this context.
     * @return True if batch processing mode is enabled, false otherwise.
     */
    @Deprecated
    public boolean isBatchModeEnabled() {
        return mode != null && mode == Mode.BATCH_EDIT;
    }

    /**
     * Reload an entity from the database into the cache. This method will return a reference to the "attached"
     * entity. This means changes to the entity will be tracked and persisted to the database.
     *
     * @param entity The entity to reload
     * @param <E> The class of the enity. The entity must implement the {@link ReloadableEntity} interface.
     * @return A (possibly) <b>NEW</b> reference to the entity that should be used for further processing.
     * @throws SQLException When reloading the entity from the database fails.
     */
    @SuppressWarnings("unchecked")
    public <E extends ReloadableEntity> E reloadEntity(E entity) throws SQLException {
        return (E) dbConnection.reloadEntity(entity);
    }

    /**
     * Remove an entity from the cache. This is necessary when batch processing a large number of items.
     *
     * @param entity The entity to reload
     * @param <E> The class of the enity. The entity must implement the {@link ReloadableEntity} interface.
     * @throws SQLException When reloading the entity from the database fails.
     */
    @SuppressWarnings("unchecked")
    public <E extends ReloadableEntity> void uncacheEntity(E entity) throws SQLException {
        dbConnection.uncacheEntity(entity);
    }

    public Boolean getCachedAuthorizationResult(DSpaceObject dspaceObject, int action, EPerson eperson) {
        if(isReadOnly()) {
            return readOnlyCache.getCachedAuthorizationResult(dspaceObject, action, eperson);
        } else {
            return null;
        }
    }

    public void cacheAuthorizedAction(DSpaceObject dspaceObject, int action, EPerson eperson, Boolean result, ResourcePolicy rp) {
        if(isReadOnly()) {
            readOnlyCache.cacheAuthorizedAction(dspaceObject, action, eperson, result);
            try {
                uncacheEntity(rp);
            } catch (SQLException e) {
                log.warn("Unable to uncache a resource policy when in read-only mode", e);
            }
        }
    }

    public Boolean getCachedGroupMembership(Group group, EPerson eperson) {
        if(isReadOnly()) {
            return readOnlyCache.getCachedGroupMembership(group, eperson);
        } else {
            return null;
        }
    }

    public void cacheGroupMembership(Group group, EPerson eperson, Boolean isMember) {
        if(isReadOnly()) {
            readOnlyCache.cacheGroupMembership(group, eperson, isMember);
        }
    }

    public void cacheAllMemberGroupsSet(EPerson ePerson, Set<Group> groups) {
        if(isReadOnly()) {
            readOnlyCache.cacheAllMemberGroupsSet(ePerson, groups);
        }
    }

    public Set<Group> getCachedAllMemberGroupsSet(EPerson ePerson) {
        if(isReadOnly()) {
            return readOnlyCache.getCachedAllMemberGroupsSet(ePerson);
        } else {
            return null;
        }
    }

    /**
     * Reload all entities related to this context.
     * @throws SQLException When reloading one of the entities fails.
     */
    private void reloadContextBoundEntities() throws SQLException {
        currentUser = reloadEntity(currentUser);
    }
}
