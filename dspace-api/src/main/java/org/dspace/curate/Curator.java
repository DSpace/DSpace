/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Curator orchestrates and manages the application of a one or more curation
 * tasks to a DSpace object. It provides common services and runtime
 * environment to the tasks.
 *
 * @author richardrodgers
 */
public class Curator {
    // status code values
    /**
     * Curator unable to find requested task
     */
    public static final int CURATE_NOTASK = -3;
    /**
     * no assigned status code - typically because task not yet performed
     */
    public static final int CURATE_UNSET = -2;
    /**
     * task encountered an error in processing
     */
    public static final int CURATE_ERROR = -1;
    /**
     * task completed successfully
     */
    public static final int CURATE_SUCCESS = 0;
    /**
     * task failed
     */
    public static final int CURATE_FAIL = 1;
    /**
     * task was not applicable to passed object
     */
    public static final int CURATE_SKIP = 2;

    /** invocation modes - used by {@link Suspendable} tasks */
    public static enum Invoked {
        INTERACTIVE, BATCH, ANY
    }

    // transaction scopes
    public static enum TxScope {
        OBJECT, CURATION, OPEN
    }

    private static final Logger log = LogManager.getLogger();

    protected final Map<String, String> runParameters = new HashMap<>();
    protected Map<String, TaskRunner> trMap = new HashMap<>();
    protected List<String> perfList = new ArrayList<>();
    protected TaskQueue taskQ = null;
    protected Appendable reporter = null;
    protected Invoked iMode = null;
    protected TaskResolver resolver = new TaskResolver();
    protected TxScope txScope = TxScope.OPEN;
    protected CommunityService communityService;
    protected DSpaceObjectUtils dspaceObjectUtils;
    protected ItemService itemService;
    protected HandleService handleService;
    protected DSpaceRunnableHandler handler;
    protected int batchSize =
            DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("curate.batch.size");

    /**
     * constructor that uses an handler for logging
     * 
     * @param handler {@code DSpaceRunnableHandler} used to logs infos
     */
    public Curator(DSpaceRunnableHandler handler) {
        this();
        this.handler = handler;
    }

    /**
     * No-arg constructor
     */
    public Curator() {
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        dspaceObjectUtils = UtilServiceFactory.getInstance().getDSpaceObjectUtils();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
        resolver = new TaskResolver();
    }

    /**
     * Set a parameter visible to all tasks in this Curator instance.
     * @param name the parameter's name.
     * @param value the parameter's value.
     */
    public void addParameter(String name, String value) {
        runParameters.put(name, value);
    }

    /**
     * Set many parameters visible to all tasks in this Curator instance.
     * @param parameters parameter name/value pairs.
     */
    public void addParameters(Map<String, String> parameters) {
        runParameters.putAll(parameters);
    }

    /**
     * Look up a run parameter.
     * @param name the name of the desired parameter.
     * @return the value of the named parameter.
     */
    public String getRunParameter(String name) {
        return runParameters.get(name);
    }

    /**
     * Add a task to the set to be performed. Caller should make no assumptions
     * on execution ordering.
     *
     * @param taskName - logical name of task
     * @return this curator - to support concatenating invocation style
     */
    public Curator addTask(String taskName) {
        ResolvedTask task = resolver.resolveTask(taskName);
        if (task != null) {
            try {
                task.init(this);
                trMap.put(taskName, new TaskRunner(task));
                // performance order currently FIFO - to be revisited
                perfList.add(taskName);
            } catch (IOException ioE) {
                System.out.println("Task: '" + taskName + "' initialization failure: " + ioE.getMessage());
            }
        } else {
            System.out.println("Task: '" + taskName + "' does not resolve");
        }
        return this;
    }

    /**
     * Returns whether this curator has the specified task
     *
     * @param taskName - logical name of the task
     * @return true if task has been configured, else false
     */
    public boolean hasTask(String taskName) {
        return perfList.contains(taskName);
    }

    /**
     * Removes a task from the set to be performed.
     *
     * @param taskName - logical name of the task
     * @return this curator - to support concatenating invocation style
     */
    public Curator removeTask(String taskName) {
        trMap.remove(taskName);
        perfList.remove(taskName);
        return this;
    }

    /**
     * Assigns invocation mode.
     *
     * @param mode one of INTERACTIVE, BATCH, ANY
     * @return the Curator instance.
     */
    public Curator setInvoked(Invoked mode) {
        iMode = mode;
        return this;
    }

    /**
     * Sets the reporting stream for this curator.
     *
     * @param reporter name of reporting stream. The name '-'
     *                 causes reporting to standard out.
     * @return return self (Curator instance) with reporter set
     */
    public Curator setReporter(Appendable reporter) {
        this.reporter = reporter;
        return this;
    }


    /**
     * Defines the transactional scope of curator executions.
     * The default is 'open' meaning that no commits are
     * performed by the framework during curation. A scope of
     * 'curation' means that a single commit will occur after the
     * entire performance is complete, and a scope of 'object'
     * will commit for each object (e.g. item) encountered in
     * a given execution.
     *
     * @param scope transactional scope
     * @return return self (Curator instance) with given scope set
     */
    public Curator setTransactionScope(TxScope scope) {
        txScope = scope;
        return this;
    }

    /**
     * Performs all configured tasks upon object identified by id. If
     * the object can be resolved as a handle, the DSO will be the
     * target object.
     *
     * <p>
     * Note:  this method has the side-effect of setting this instance's Context
     * reference.  The setting is retained on return.
     *
     * @param c  a DSpace context
     * @param id an object identifier
     * @throws IOException if IO error
     */
    public void curate(Context c, String id) throws IOException {
        if (id == null) {
            throw new IOException("Cannot perform curation task(s) on a null object identifier!");
        }
        try {
            DSpaceObject dso = dspaceObjectUtils.findDSpaceObject(c,id);
            if (dso != null) {
                curate(c, dso);
            } else {
                for (String taskName : perfList) {
                    trMap.get(taskName).run(c, id);
                }
            }
            // if curation scoped, commit transaction
            if (txScope.equals(TxScope.CURATION)) {
                if (c != null) {
                    c.complete();
                }
            }
        } catch (SQLException sqlE) {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
    }

    /**
     * Performs all configured tasks upon DSpace object
     * (Community, Collection or Item).
     * @param dso the DSpace object
     * @throws IOException if IO error
     */
    public void curate(Context c, DSpaceObject dso) throws IOException {
        if (dso == null) {
            throw new IOException("Cannot perform curation task(s) on a null DSpaceObject!");
        }
        int type = dso.getType();
        for (String taskName : perfList) {
            TaskRunner tr = trMap.get(taskName);
            // do we need to iterate over the object ?
            if (type == Constants.ITEM || tr.task.isDistributive()) {
                tr.run(c, dso);
            } else if (type == Constants.COLLECTION) {
                doCollection(c, tr, (Collection) dso);
            } else if (type == Constants.COMMUNITY) {
                doCommunity(c, tr, (Community) dso);
            } else if (type == Constants.SITE) {
                doSite(c, tr, (Site) dso);
            }
        }
    }

    /**
     * Places a curation request for the object identified by id on a
     * managed queue named by the queueId.
     *
     * @param c       A DSpace context
     * @param id      an object Id
     * @param queueId name of a queue. If queue does not exist, it will
     *                be created automatically.
     * @throws IOException if IO error
     */
    public void queue(Context c, String id, String queueId) throws IOException {
        if (taskQ == null) {
            taskQ = (TaskQueue) CoreServiceFactory.getInstance().getPluginService().getSinglePlugin(TaskQueue.class);
        }
        if (taskQ != null) {
            taskQ.enqueue(queueId, new TaskQueueEntry(c.getCurrentUser().getName(),
                                                      Instant.now().toEpochMilli(), perfList, id));
        } else {
            System.out.println("curate - no TaskQueue implemented");
        }
    }

    /**
     * Removes all configured tasks from the Curator.
     */
    public void clear() {
        trMap.clear();
        perfList.clear();
    }

    /**
     * Adds a message to the configured reporting stream.
     *
     * @param message the message to output to the reporting stream.
     */
    public void report(String message) {
        if (null == reporter) {
            logWarning("report called with no Reporter set:  {}", message);
            return;
        }

        try {
            reporter.append(message);
        } catch (IOException ex) {
            System.out.println("Task reporting failure: " +  ex);
        }
    }

    /**
     * Returns the status code for the latest performance of the named task.
     *
     * @param taskName the task name
     * @return the status code - one of CURATE_ values
     */
    public int getStatus(String taskName) {
        TaskRunner tr = trMap.get(taskName);
        return (tr != null) ? tr.statusCode : CURATE_NOTASK;
    }

    /**
     * Returns the result string for the latest performance of the named task.
     *
     * @param taskName the task name
     * @return the result string, or <code>null</code> if task has not set it.
     */
    public String getResult(String taskName) {
        TaskRunner tr = trMap.get(taskName);
        return (tr != null) ? tr.result : null;
    }

    /**
     * Assigns a result to the performance of the named task.
     *
     * @param taskName the task name
     * @param result   a string indicating results of performing task.
     */
    public void setResult(String taskName, String result) {
        TaskRunner tr = trMap.get(taskName);
        if (tr != null) {
            tr.setResult(result);
        }
    }

    /**
     * Returns whether a given DSO is a 'container' - collection or community
     *
     * @param dso a DSpace object
     * @return true if a container, false otherwise
     */
    public static boolean isContainer(DSpaceObject dso) {
        return (dso.getType() == Constants.COMMUNITY ||
            dso.getType() == Constants.COLLECTION);
    }

    /**
     * Run task for entire Site (including all Communities, Collections and Items)
     *
     * @param tr   TaskRunner
     * @param site DSpace Site object
     * @return true if successful, false otherwise
     * @throws IOException if IO error
     */
    protected boolean doSite(Context context, TaskRunner tr, Site site) throws IOException {
        try {

            // Site-wide Tasks really should have an EPerson performer associated with them,
            // otherwise they are run as an "anonymous" user with limited access rights.
            if (context.getCurrentUser() == null && !context.ignoreAuthorization()) {
                logWarning("You are running one or more Site-Wide curation tasks in ANONYMOUS USER mode," +
                             " as there is no EPerson 'performer' associated with this task. To associate an EPerson " +
                             "'performer' " +
                             " you should ensure tasks are called via the Curator.curate(Context, ID) method.");
            }

            //Run task for the Site object itself
            if (!tr.run(context, site)) {
                return false;
            }

            //Then, perform this task for all Top-Level Communities in the Site
            // (this will recursively perform task for all objects in DSpace)
            for (Community subcomm : communityService.findAllTop(context)) {
                subcomm = context.reloadEntity(subcomm);
                if (!doCommunity(context, tr, subcomm)) {
                    return false;
                }
                context.commit();
            }
        } catch (SQLException sqlE) {
            throw new IOException(sqlE);
        }

        return true;
    }

    /**
     * Run task for Community along with all sub-communities and collections.
     *
     * @param tr   TaskRunner
     * @param comm Community
     * @return true if successful, false otherwise
     * @throws IOException if IO error
     */
    protected boolean doCommunity(Context context, TaskRunner tr, Community comm) throws IOException {
        try {
            if (!tr.run(context, comm)) {
                return false;
            }
            for (Community subcomm : comm.getSubcommunities()) {
                subcomm = context.reloadEntity(subcomm);
                if (!doCommunity(context, tr, subcomm)) {
                    return false;
                }
                context.commit();
            }
            comm = context.reloadEntity(comm);
            for (Collection coll : comm.getCollections()) {
                coll = context.reloadEntity(coll);
                if (!doCollection(context, tr, coll)) {
                    return false;
                }
                context.commit();
            }
        }  catch (SQLException sqlE) {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
        return true;
    }

    /**
     * Run task for Collection along with all Items in that collection.
     *
     * @param tr   TaskRunner
     * @param coll Collection
     * @return true if successful, false otherwise
     * @throws IOException if IO error
     */
    private Integer total = null;
    private int current = 0;
    private Date start = null;
    private Date last = null;
    private final DecimalFormat df = new DecimalFormat("0.00");
    protected boolean doCollection(Context context, TaskRunner tr, Collection coll) throws IOException {
        try {
            if (!tr.run(context, coll)) {
                return false;
            }
            if (total == null) {
                total = itemService.countTotal(context);
                start = new Date();
                last = start;
            }

            int offset = 0;
            boolean hasNext;
            do {
                coll = context.reloadEntity(coll);
                Iterator<Item> iter = itemService.findByCollection(context, coll, batchSize, offset);
                hasNext = iter.hasNext();
                while (iter.hasNext()) {
                    Item item = iter.next();
                    offset++;
                    boolean shouldContinue = tr.run(context, item);
                    if (!shouldContinue) {
                        return false;
                    }
                    if (++current % 100 == 0) {
                        Date now = new Date();
                        float ms = (float) (now.getTime() - last.getTime());
                        float avgSeconds = (float) (now.getTime() - start.getTime()) / 1000 / (current / 100);
                        System.out.println(current + " / " + total + " -- " + ms +
                                "ms - (avg " + df.format(avgSeconds) + "s per 100)");
                        last = now;
                    }
                }
                context.commit();
            } while (hasNext);
        } catch (SQLException sqlE) {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
        return true;
    }

    /**
     * Record a 'visit' to a DSpace object and enforce any policies set
     * on this curator.
     *
     * @throws IOException A general class of exceptions produced by failed or interrupted I/O operations.
     */
    protected void visit(Context context) throws IOException {
        if (context != null) {
            if (txScope.equals(TxScope.OBJECT)) {
                context.dispatchEvents();
            }
        }
    }

    protected class TaskRunner {
        ResolvedTask task = null;
        int statusCode = CURATE_UNSET;
        String result = null;

        public TaskRunner(ResolvedTask task) {
            this.task = task;
        }

        public boolean run(Context context, DSpaceObject dso) throws IOException {
            try {
                if (dso == null) {
                    throw new IOException("DSpaceObject is null");
                }
                String id = (dso.getHandle() != null) ? dso.getHandle() : "workflow item: " + dso.getID();
                statusCode = task.perform(context, dso);
                logInfo(logMessage(id));
                visit(context);
                return !suspend(statusCode);
            } catch (IOException ioe) {
                //log error & pass exception upwards
                System.out.println("Error executing curation task '" + task.getName() + "'; " + ioe);
                throw ioe;
            }
        }

        public boolean run(Context context, String id) throws IOException {
            try {
                if (context == null || id == null) {
                    throw new IOException("Context or identifier is null");
                }
                statusCode = task.perform(context, id);
                logInfo(logMessage(id));
                visit(context);
                return !suspend(statusCode);
            } catch (IOException ioe) {
                //log error & pass exception upwards
                System.out.println("Error executing curation task '" + task.getName() + "'; " + ioe);
                throw ioe;
            }
        }

        public void setResult(String result) {
            this.result = result;
        }

        protected boolean suspend(int code) {
            Invoked mode = task.getMode();
            if (mode != null && (mode.equals(Invoked.ANY) || mode.equals(iMode))) {
                for (int i : task.getCodes()) {
                    if (code == i) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Builds a useful log message for a curation task.
         *
         * @param id ID of DSpace Object
         * @return log message text
         */
        protected String logMessage(String id) {
            StringBuilder mb = new StringBuilder();
            mb.append("Curation task: ").append(task.getName()).
                append(" performed on: ").append(id).
                  append(" with status: ").append(statusCode);
            if (result != null) {
                mb.append(". Result: '").append(result).append("'");
            }
            return mb.toString();
        }

        /**
         * Proxy method for logging with INFO level
         * 
         * @param message that needs to be logged
         */
        protected void logInfo(String message) {
            if (handler == null) {
                log.info(message);
            } else {
                handler.logInfo(message);
            }
        }

    }

    /**
     * Proxt method for logging with WARN level
     * 
     * @param message
     */
    protected void logWarning(String message) {
        logWarning(message, null);
    }

    /**
     * Proxy method for logging with WARN level and a {@code Messageformatter}
     * that generates the final log.
     * 
     * @param message Target message to format or print
     * @param object  Object to use inside the message, or null
     */
    protected void logWarning(String message, Object object) {
        if (handler == null) {
            if (object != null) {
                log.warn(message, object);
            } else {
                log.warn(message);
            }
        } else {
            if (object != null) {
                handler.logWarning(MessageFormat.format(message, object));
            } else {
                handler.logWarning(message);
            }
        }
    }
}
