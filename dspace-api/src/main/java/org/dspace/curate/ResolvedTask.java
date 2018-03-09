/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * ResolvedTask wraps an implementation of one of the CurationTask or
 * ScriptedTask interfaces and provides for uniform invocation based on
 * CurationTask methods.
 *
 * @author richardrodgers
 */
public class ResolvedTask
{
	// wrapped objects
	private CurationTask cTask;
	private ScriptedTask sTask;
	// local name of task
	private String taskName;
	// annotation data
	private boolean distributive = false;
	private boolean mutative = false;
	private Curator.Invoked mode = null;
    private int[] codes = null;
	
	
	protected ResolvedTask(String taskName, CurationTask cTask)
	{
		this.taskName = taskName;
		this.cTask = cTask;
		// process annotations
		Class ctClass = cTask.getClass();
		distributive = ctClass.isAnnotationPresent(Distributive.class);
		mutative = ctClass.isAnnotationPresent(Mutative.class);
		Suspendable suspendAnno = (Suspendable)ctClass.getAnnotation(Suspendable.class);
        if (suspendAnno != null)
        {
            mode = suspendAnno.invoked();
            codes = suspendAnno.statusCodes();
        }
	}
	
	protected ResolvedTask(String taskName, ScriptedTask sTask)
	{
		this.taskName = taskName;
		this.sTask = sTask;
		// annotation processing TBD
	}
	
    /**
     * Initialize task - parameters inform the task of it's invoking curator.
     * Since the curator can provide services to the task, this represents
     * curation DI.
     * 
     * @param curator the Curator controlling this task
     * @throws IOException if IO error
     */
    public void init(Curator curator) throws IOException
    {
    	if (unscripted())
    	{
    		cTask.init(curator, taskName);
    	}
    	else
    	{
    		sTask.init(curator, taskName);
    	}
    }

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @return status code
     * @throws IOException if error
     */
    public int perform(DSpaceObject dso) throws IOException
    {
    	return (unscripted()) ? cTask.perform(dso) : sTask.performDso(dso);
    }

    /**
     * Perform the curation task for passed id
     * 
     * @param ctx DSpace context object
     * @param id persistent ID for DSpace object
     * @return status code
     * @throws IOException if error
     */
    public int perform(Context ctx, String id) throws IOException
    {
    	return (unscripted()) ? cTask.perform(ctx, id) : sTask.performId(ctx, id);	
    }
    
    /**
     * Returns local name of task
     * @return name
     *         the local name of the task
     */
    public String getName()
    {
    	return taskName;
    }
    
    /**
     * Returns whether task should be distributed through containers
     * 
     */
    public boolean isDistributive()
    {
    	return distributive;
    }
    
    /**
     * Returns whether task alters (mutates) it's target objects
     * 
     */
    public boolean isMutative()
    {
    	return mutative;
    }
    
    public Curator.Invoked getMode()
    {
    	return mode;
    }
    
    public int[] getCodes()
    {
    	return codes;
    }
    
    private boolean unscripted()
    {
    	return sTask == null;
    }
}
