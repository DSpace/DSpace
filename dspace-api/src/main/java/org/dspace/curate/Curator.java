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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

/**
 * Curator orchestrates and manages the application of a one or more curation
 * tasks to a DSpace object. It provides common services and runtime
 * environment to the tasks.
 * 
 * @author richardrodgers
 */
public class Curator
{
    // status code values
    /** Curator unable to find requested task */
    public static final int CURATE_NOTASK = -3;
    /** no assigned status code - typically because task not yet performed */
    public static final int CURATE_UNSET = -2;
    /** task encountered a error in processing */
    public static final int CURATE_ERROR = -1;
    /** task completed successfully */
    public static final int CURATE_SUCCESS = 0;
    /** task failed */
    public static final int CURATE_FAIL = 1;
    /** task was not applicable to passed object */
    public static final int CURATE_SKIP = 2;
    
    // invocation modes - used by Suspendable tasks
    public static enum Invoked { INTERACTIVE, BATCH, ANY };

    private static Logger log = Logger.getLogger(Curator.class);
    
    private static final ThreadLocal<Integer> performer = new ThreadLocal<Integer>();
    
    private Map<String, TaskRunner> trMap = new HashMap<String, TaskRunner>();
    private List<String> perfList = new ArrayList<String>();
    private TaskQueue taskQ = null;
    private String reporter = null;
    private Invoked iMode = null;

    /**
     * No-arg constructor
     */
    public Curator()
    {
    }

    /**
     * Add a task to the set to be performed. Caller should make no assumptions
     * on execution ordering.
     * 
     * @param taskName - logical name of task
     * @return this curator - to support concatenating invocation style
     */
    public Curator addTask(String taskName)
    {
        CurationTask task = TaskResolver.resolveTask(taskName);
        if (task != null)
        {
            try
            {
                task.init(this, taskName);
                trMap.put(taskName, new TaskRunner(task, taskName));
                // performance order currently FIFO - to be revisited
                perfList.add(taskName);
            }
            catch (IOException ioE)
            {
               log.error("Task: '" + taskName + "' initialization failure: " + ioE.getMessage()); 
            }
        }
        else
        {
            log.error("Task: '" + taskName + "' does not resolve");
        }
        return this;
    }
    
    /**
     * Returns whether this curator has the specified task
     * 
     * @param taskName - logical name of the task
     * @return true if task has been configured, else false
     */
     public boolean hasTask(String taskName)
     {
         return perfList.contains(taskName);
     }
    
    
    /**
     * Removes a task from the set to be performed.
     * 
     * @param taskName - logical name of the task
     * @return this curator - to support concatenating invocation style
     */
    public Curator removeTask(String taskName)
    {
        trMap.remove(taskName);
        perfList.remove(taskName);
        return this;
    }
    
    /**
     * Assigns invocation mode.
     * 
     * @param mode one of INTERACTIVE, BATCH, ANY
     * @return
     */
    public Curator setInvoked(Invoked mode)
    {
        iMode = mode;
        return this;
    }

    /**
     * Sets the reporting stream for this curator.
     * 
     * @param reporter name of reporting stream. The name '-'
     *                 causes reporting to standard out. 
     * @return the Curator instance
     */
    public Curator setReporter(String reporter)
    {
        this.reporter = reporter;
        return this;
    }

    /**
     * Performs all configured tasks upon object identified by id. If
     * the object can be resolved as a handle, the DSO will be the
     * target object.
     * 
     * @param c a Dpace context
     * @param id an object identifier
     * @throws IOException
     */
    public void curate(Context c, String id) throws IOException
    {
        if (id == null)
        {
           throw new IOException("Cannot perform curation task(s) on a null object identifier!");            
        }
        try
        {
            //Save the currently authenticated user's ID to the current Task thread
            //(Allows individual tasks to retrieve current user info via currentPerformer() method)
            if(c.getCurrentUser()!=null)
            {    
                performer.set(Integer.valueOf(c.getCurrentUser().getID()));
            }
           
            DSpaceObject dso = HandleManager.resolveToObject(c, id);
            if (dso != null)
            {
                curate(dso);
            }
            else
            {
                for (String taskName : perfList)
                {
                    trMap.get(taskName).run(c, id);
                }
            }
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
        finally
        {
            performer.remove();
        }
    }

    /**
     * Performs all configured tasks upon DSpace object 
     * (Community, Collection or Item).
     * <P>
     * Note: Site-wide tasks will default to running as
     * an Anonymous User unless you call the Site-wide task
     * via the 'curate(Context,String)' method with an 
     * authenticated Context object.
     * 
     * @param dso the DSpace object
     * @throws IOException
     */
    public void curate(DSpaceObject dso) throws IOException
    {
        if (dso == null)
        {
            throw new IOException("Cannot perform curation task(s) on a null DSpaceObject!");
        }
        int type = dso.getType();
        for (String taskName : perfList)
        {
            TaskRunner tr = trMap.get(taskName);
            // do we need to iterate over the object ?
            if (type == Constants.ITEM ||
                tr.task.getClass().isAnnotationPresent(Distributive.class))
            {
                tr.run(dso);
            }
            else if (type == Constants.COLLECTION)
            {
                doCollection(tr, (Collection)dso);
            }
            else if (type == Constants.COMMUNITY)
            {
                doCommunity(tr, (Community)dso);
            }  
            else if (type == Constants.SITE)
            {
                doSite(tr, (Site) dso);    
            }
        }
    }
    
    /**
     * Places a curation request for the object identified by id on a
     * managed queue named by the queueId.
     * 
     * @param c A DSpace context
     * @param id an object Id
     * @param queueId name of a queue. If queue does not exist, it will
     *                be created automatically.
     * @throws IOException
     */
    public void queue(Context c, String id, String queueId) throws IOException
    {
        if (taskQ == null)
        {
            taskQ = (TaskQueue)PluginManager.getSinglePlugin("curate", TaskQueue.class);
        }
        if (taskQ != null)
        {
            taskQ.enqueue(queueId, new TaskQueueEntry(c.getCurrentUser().getName(),
                                    System.currentTimeMillis(), perfList, id));
        }
        else
        {
            log.error("curate - no TaskQueue implemented");
        }
    }
    
    /**
     * Removes all configured tasks from the Curator.
     */
    public void clear()
    {
        trMap.clear();
        perfList.clear();
    }

    /**
     * Adds a message to the configured reporting stream.
     * 
     * @param message the message to output to the reporting stream.
     */
    public void report(String message)
    {
        // Stub for now
        if ("-".equals(reporter))
        {
            System.out.println(message);
        }
    }

    /**
     * Returns the status code for the latest performance of the named task.
     * 
     * @param taskName the task name
     * @return the status code - one of CURATE_ values
     */
    public int getStatus(String taskName)
    {
        TaskRunner tr = trMap.get(taskName);
        return (tr != null) ? tr.statusCode : CURATE_NOTASK;
    }

    /**
     * Returns the result string for the latest performance of the named task.
     * 
     * @param taskName the task name
     * @return the result string, or <code>null</code> if task has not set it.
     */
    public String getResult(String taskName)
    {
        TaskRunner tr = trMap.get(taskName);
        return (tr != null) ? tr.result : null;
    }

    /**
     * Assigns a result to the performance of the named task.
     * 
     * @param taskName the task name
     * @param result a string indicating results of performing task.
     */
    public void setResult(String taskName, String result)
    {
        TaskRunner tr = trMap.get(taskName);
        if (tr != null)
        {
            tr.setResult(result);
        }
    }

    /**
     * Returns the Eperson ID of the Context currently in use in performing a task,
     * if known, else <code>null</code>. 
     * <P>
     * In many circumstances, this value will be null: 
     * when the curator is not in the perform method, when curation
     * is invoked with a DSO (the context is 'hidden'). 
     * <P>
     * The primary intended use for this method is to ensure individual tasks,
     * which may need to create a new Context, can also properly initialize that
     * Context with an EPerson ID (to ensure proper access rights exist in that Context).
     * <P>
     * Current performer information is also used when executing Site-Wide tasks 
     * (see Curator.doSite() method).
     */
    public static Integer currentPerformer()
    {
    	return performer.get();
    }
    
    /**
     * Returns a Context object which is "authenticated" as the current
     * EPerson performer (see 'currentPerformer()' method). This is primarily a 
     * utility method to allow tasks access to an authenticated Context when 
     * necessary.
     * <P>
     * If the 'currentPerformer()' is null or not set, then this just returns
     * a brand new Context object representing an Anonymous User.
     * 
     * @return authenticated Context object (or anonymous Context if currentPerformer() is null)
     */
    public static Context authenticatedContext()
            throws SQLException
    {
    	//Create a new context
        Context ctx = new Context();
        
        Integer epersonID = currentPerformer();
            
        //If a Curator 'performer' ID is set
        if(epersonID!=null)
        {    
            //parse the performer's User ID & set as the currently authenticated user in Context
            EPerson autenticatedUser = EPerson.find(ctx, epersonID.intValue());
            ctx.setCurrentUser(autenticatedUser);
        }
        else
        {
            //otherwise, no-op. This is the equivalent of an ANONYMOUS USER Context
        }
        
        return ctx;
    }

    /**
     * Returns whether a given DSO is a 'container' - collection or community
     * @param dso a DSpace object
     * @return true if a container, false otherwise
     */
    public static boolean isContainer(DSpaceObject dso)
    {
        return (dso.getType() == Constants.COMMUNITY ||
                dso.getType() == Constants.COLLECTION);
    }

    /**
     * Run task for entire Site (including all Communities, Collections & Items)
     * @param tr TaskRunner
     * @param site DSpace Site object
     * @return true if successful, false otherwise
     * @throws IOException 
     */
    private boolean doSite(TaskRunner tr, Site site) throws IOException
    {
        Context ctx = null;
        try
        {
            // Site-wide Tasks really should have an EPerson performer associated with them,
            // otherwise they are run as an "anonymous" user with limited access rights.
            if(Curator.currentPerformer()==null)
            {
                log.warn("You are running one or more Site-Wide curation tasks in ANONYMOUS USER mode," +
                         " as there is no EPerson 'performer' associated with this task. To associate an EPerson 'performer' " +
                         " you should ensure tasks are called via the Curator.curate(Context, ID) method.");
            }
            else
            {
                // Create a new Context for this Sitewide task, authenticated as the current task performer.
                ctx = Curator.authenticatedContext();
            }
            
            //Run task for the Site object itself
            if (! tr.run(site))
            {
                return false;
            }
            
            //Then, perform this task for all Top-Level Communities in the Site
            // (this will recursively perform task for all objects in DSpace)
            for (Community subcomm : Community.findAllTop(ctx))
            {
                if (! doCommunity(tr, subcomm))
                {
                    return false;
                }
            }
            
            //complete & close our created Context
            ctx.complete();
        }
        catch (SQLException sqlE)
        {
            //abort Context & all changes
            if(ctx!=null)
                ctx.abort();
            
            throw new IOException(sqlE);
        }

        return true;
    }
    
    /**
     * Run task for Community along with all sub-communities and collections.
     * @param tr TaskRunner
     * @param comm Community
     * @return true if successful, false otherwise
     * @throws IOException 
     */
    private boolean doCommunity(TaskRunner tr, Community comm) throws IOException
    {
        try
        {
            if (! tr.run(comm))
            {
                return false;
            }
            for (Community subcomm : comm.getSubcommunities())
            {
                if (! doCommunity(tr, subcomm))
                {
                    return false;
                }
            }
            for (Collection coll : comm.getCollections())
            {
                if (! doCollection(tr, coll))
                {
                    return false;
                }
            }
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
        return true;
    }

    /**
     * Run task for Collection along with all Items in that collection.
     * @param tr TaskRunner
     * @param coll Collection
     * @return true if successful, false otherwise
     * @throws IOException 
     */
    private boolean doCollection(TaskRunner tr, Collection coll) throws IOException
    {
        try
        {
            if (! tr.run(coll))
            {
                return false;
            }
            ItemIterator iter = coll.getItems();
            while (iter.hasNext())
            {
                if (! tr.run(iter.next()))
                {
                    return false;
                }
            }
        }
        catch (SQLException sqlE)
        {
            throw new IOException(sqlE.getMessage(), sqlE);
        }
        return true;
    }

    private class TaskRunner
    {
        CurationTask task = null;
        String taskName = null;
        int statusCode = CURATE_UNSET;
        String result = null;
        Invoked mode = null;
        int[] codes = null;

        public TaskRunner(CurationTask task, String name)
        {
            this.task = task;
            taskName = name;
            parseAnnotations(task.getClass());
        }
        
        public boolean run(DSpaceObject dso) throws IOException
        {
            try
            {    
                if (dso == null)
                {
                    throw new IOException("DSpaceObject is null");
                }
                statusCode = task.perform(dso);
                String id = (dso.getHandle() != null) ? dso.getHandle() : "workflow item: " + dso.getID();
                log.info(logMessage(id));

                return ! suspend(statusCode);
            }
            catch(IOException ioe)
            {
                //log error & pass exception upwards
                log.error("Error executing curation task '" + taskName + "'", ioe);
                throw ioe;
            }
        }
        
        public boolean run(Context c, String id) throws IOException
        {
            try
            {
                if (c == null || id == null)
                {
                    throw new IOException("Context or identifier is null");
                }
                statusCode = task.perform(c, id);
                log.info(logMessage(id));

                return ! suspend(statusCode);
            }
            catch(IOException ioe)
            {
                //log error & pass exception upwards
                log.error("Error executing curation task '" + taskName + "'", ioe);
                throw ioe;
            }
        }

        public void setResult(String result)
        {
            this.result = result;
        }
        
        private void parseAnnotations(Class tClass)
        {
            Suspendable suspendAnn = (Suspendable)tClass.getAnnotation(Suspendable.class);
            if (suspendAnn != null)
            {
                mode = suspendAnn.invoked();
                codes = suspendAnn.statusCodes();
            }
        }
        
        private boolean suspend(int code)
        {
            if (mode != null && (mode.equals(Invoked.ANY) || mode.equals(iMode)))
            {
                for (int i : codes)
                {
                    if (code == i)
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        
        /**
         * Builds a useful log message for a curation task.
         * @param id ID of DSpace Object
         * @return log message text
         */
        private String logMessage(String id) 
        {
            StringBuilder mb = new StringBuilder();
            mb.append("Curation task: ").append(taskName).
               append(" performed on: ").append(id).
               append(" with status: ").append(statusCode);
            if (result != null)
            {
                mb.append(". Result: '").append(result).append("'");
            }
            return mb.toString();
        }
    }
}
