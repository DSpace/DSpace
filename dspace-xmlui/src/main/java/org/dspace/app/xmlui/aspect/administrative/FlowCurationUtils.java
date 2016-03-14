/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cocoon.environment.Request;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.curate.Curator;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Select;

/**
 *
 * @author wbossons
 */
public class FlowCurationUtils
{
   /** Language Strings */
    private static final Message T_curate_success_notice =
            new Message("default","xmlui.administrative.FlowCurationUtils.curate_success_notice");
    private static final Message T_curate_fail_notice =
            new Message("default","xmlui.administrative.FlowCurationUtils.curate_failed_notice");
    private static final Message T_queue_success_notice =
            new Message("default","xmlui.administrative.FlowCurationUtils.queue_success_notice");
    private static final Message T_queue_fail_notice =
            new Message("default","xmlui.administrative.FlowCurationUtils.queue_failed_notice");
    
    
    private static final Map<String, String> map = new HashMap<String, String>();
    
    protected static Curator getCurator(String taskName)
    {
        if (taskName != null && taskName.length() == 0)
        {
            taskName = null;
        }
        Curator curator = new Curator();
        curator.addTask(taskName);
        curator.setInvoked(Curator.Invoked.INTERACTIVE);
        return curator;
    }
    
    /**
     * Build a FlowResult which will provide a Notice to users, notifying them
     * of whether the Curation task succeeded or failed.
     * @param taskName name of Curation Task
     * @param curator active Curator
     * @param success whether it succeeded or failed
     * @return FlowResult
     */
    protected static FlowResult getRunFlowResult(String taskName, Curator curator, boolean success)
    {
        if (map.isEmpty())
        {
            String[] statusCodes = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("curate.ui.statusmessages");
            for (String pair : statusCodes)
            {
                String[] parts = pair.split("=");
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        String status = map.get(String.valueOf(curator.getStatus(taskName)));
        if (status == null)
        {
            // invalid = use string for 'other
            status = map.get("other");
        }
        String result = curator.getResult(taskName);
        FlowResult flowResult = new FlowResult();
        //set whether task succeeded or failed
        flowResult.setOutcome(success); 
        if(result==null)
            result = "Nothing to do for this DSpace object.";
        //add in status message
        if(success)
        {   
            //@TODO: Ideally, all of this status information would be contained within a translatable I18N Message.
            // Unfortunately, there currently is no support for displaying Parameterized Messages in Notices
            // (See FlowResult.getMessage(), sitemap.xmap and NoticeTransformer)
            flowResult.setHeader(new Message("default", "Task: " + getUITaskName(taskName)));
            flowResult.setMessage(T_curate_success_notice);
            flowResult.setCharacters("STATUS: " + status + ", RESULT: " + result); 
        }
        else
        {
            flowResult.setHeader(new Message("default", "Task: " + getUITaskName(taskName)));
            flowResult.setMessage(T_curate_fail_notice);
            flowResult.setCharacters("STATUS: Failure, RESULT: " + result); 
            
        }
        flowResult.setContinue(true);
        return flowResult;
    }
    
    /**
     * Build a FlowResult which will provide a Notice to users, notifying them
     * of whether the Curation task was queued successfully or not
     * @param taskName name of Curation Task
     * @param status whether it succeeded or failed
     * @param objId the DSpace object ID
     * @param queueName the name of the queue
     * @return FlowResult
     */
    protected static FlowResult getQueueFlowResult(String taskName, boolean status,
                                                   String objId, String queueName)
    {
        FlowResult flowResult = new FlowResult();
        flowResult.setOutcome(status);
        
        //add in status message
        if(status)
        {
            //@TODO: Ideally, all of this status information would be contained within a translatable I18N Message.
            // Unfortunately, there currently is no support for displaying Parameterized Messages in Notices
            // (See FlowResult.getMessage(), sitemap.xmap and NoticeTransformer)
            flowResult.setHeader(new Message("default", "Task: " + getUITaskName(taskName)));
            flowResult.setMessage(T_queue_success_notice);
            flowResult.setCharacters("RESULT: Object '" + objId + "' queued in '" + queueName + "' Queue");  
        }
        else
        {
            flowResult.setHeader(new Message("default", "Task: " + getUITaskName(taskName)));
            flowResult.setMessage(T_queue_fail_notice);
            flowResult.setCharacters("RESULT: FAILED to queue Object '" + objId + "' in '" + queueName + "' Queue");
        }
        flowResult.setContinue(true);
        return flowResult;
    }
	
    
    /**
     * Retrieve UI "friendly" Task Name for display to user
     * 
     * @param taskID the short name / identifier for the task
     * @return the User Friendly name for this task
     */
    protected static String getUITaskName(String taskID)
    {       
            String[] tasks = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("curate.ui.tasknames");

            for (String task : tasks)
            {
                //retrieve keyValuePair (format [taskID]=[UI Task Name])
                String[] keyValuePair = task.split("=");
                
                if(keyValuePair!=null && keyValuePair.length==2)
                {    
                    if(taskID.equals(keyValuePair[0].trim()))
                        return keyValuePair[1];
                }
            }
            //if we are here, the UI friendly task name was not found
            // So, we'll just return the TaskID, as it's better than nothing
            return taskID;
    }
    
    
    /**
     * Utility method to process curation tasks
     * submitted via the DSpace Admin UI Curate Form.
     * 
     * @param context current DSpace Context
     * @param request current Cocoon request
     * @return FlowResult representing the result of request
     * @see org.dspace.app.xmlui.aspect.administrative.CurateForm
     */
    public static FlowResult processCurateObject(Context context, Request request)
    {
        //get input values from Form (see org.dspace.app.xmlui.aspect.administrative.CurateForm)
        String task = request.getParameter("curate_task");
        String objHandle = request.getParameter("identifier");
        Curator curator = FlowCurationUtils.getCurator(task);

        FlowResult result = null;
        try
        {
            // Curate this object & return result
            curator.curate(context, objHandle);
            result = FlowCurationUtils.getRunFlowResult(task, curator, true);
        }
        catch (Exception e) 
        {
            curator.setResult(task, e.getMessage());
            result = FlowCurationUtils.getRunFlowResult(task, curator, false);
        }
        //pass curation task name & identifier back in FlowResult (so it can be pre-populated on UI)
        result.setParameter("curate_task", task);
        result.setParameter("identifier", objHandle);
        return result;
    }
    
    /**
     * Utility method to queue curation tasks
     * submitted via the DSpace Admin UI Curate Form.
     * 
     * @param context current DSpace Context
     * @param request current Cocoon request
     * @return FlowResult representing the result of request
     * @see org.dspace.app.xmlui.aspect.administrative.CurateForm
     */
    public static FlowResult processQueueObject(Context context, Request request)
    {
        //get input values from Form (see org.dspace.app.xmlui.aspect.administrative.CurateForm)
        String task = request.getParameter("curate_task");
        String objHandle = request.getParameter("identifier");
        
        Curator curator = FlowCurationUtils.getCurator(task);
        
        String taskQueueName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("curate.ui.queuename");
        boolean status = false;
       
        if (objHandle != null)
        {
            try
            {
                //queue the task for later processing of this object
                curator.queue(context, objHandle, taskQueueName);
                status = true;
            }
            catch (IOException ioe)
            {
                // no-op (any error should be logged by the Curator itself)
            }
        }
        return FlowCurationUtils.getQueueFlowResult(task, status, objHandle, taskQueueName);
    }
    
    /** Utility methods to support curation groups/tasks form fields
     *
     *
     */
    public static final String CURATE_TASK_NAMES  = "ui.tasknames";
    public static final String CURATE_GROUP_NAMES = "ui.taskgroups";
    public static final String CURATE_GROUP_PREFIX = "ui.taskgroup";
    public static final String UNGROUPED_TASKS    = "ungrouped";
    
    public static Map<String, String> allTasks = new LinkedHashMap<String, String>();
    public static Map<String, String[]> groupedTasks = new LinkedHashMap<String, String[]>();
    public static Map<String, String> groups = new LinkedHashMap<String, String>();
    
    public static void setupCurationTasks()
    {
    	try
    	{
    		setAllTasks();
    		setGroupedTasks();
    		setGroups();
    	}
    	catch (Exception we)
    	{
    		// noop
    	}
    }
    
    public static void setAllTasks() throws WingException, UnsupportedEncodingException
    {
    	String[] properties = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("curate." + CURATE_TASK_NAMES);
    	for (String property : properties)
    	{
             //System.out.println("set all tasks and property = " + property + "\n");
    		String[] keyValuePair = property.split("=");
            allTasks.put(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
            		    URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
    	}
    }
    
    public static void setGroups() throws WingException, UnsupportedEncodingException
    {
    	String[] properties = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("curate." + CURATE_GROUP_NAMES);
        if (properties != null)
        {
        	for (String property : properties)
                {
        		String[] keyValuePair = property.split("=");
        		groups.put(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
                          URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
        	}
        }
    }
    
    public static void setGroupedTasks() throws WingException, UnsupportedEncodingException
    {
    	if (groups.isEmpty())
    	{
    		setGroups();
    	}
    	if (!groups.isEmpty())
    	{
    		Iterator<String> iterator = groups.keySet().iterator();
    		while (iterator.hasNext())
    		{
                String key = iterator.next();
                String[] properties = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("curate." + CURATE_GROUP_PREFIX + "." + key);
                groupedTasks.put(URLDecoder.decode(key, "UTF-8"), properties);
            }
        }
    }

    public static Select getGroupSelectOptions(Select select) throws WingException
    {
    	Iterator<String> iterator = groups.keySet().iterator();
        while (iterator.hasNext())
        {
        	String key = iterator.next();
            select.addOption(key, groups.get(key));
        }
        return select;
    }
    
    public static Select getTaskSelectOptions(Select select, String curateGroup) throws WingException
    {
    	String key;
    	String[] values = null;
        Iterator<String> iterator = null;
        if (groupedTasks.isEmpty())
        {
        	iterator = allTasks.keySet().iterator();
            while (iterator.hasNext())
            {
            	key = iterator.next();
                select.addOption(key, allTasks.get(key));
            }
            return select;
        }
        else
        {
        	iterator = groupedTasks.keySet().iterator();
        }
        while (iterator.hasNext())
        {
        	key = iterator.next();
            values = groupedTasks.get(key);
            if (key.equals(curateGroup))
            {
            	for (String value : values)
                {
                    Iterator<String> innerIterator = allTasks.keySet().iterator();
                    while (innerIterator.hasNext())
                    {
                    	String optionValue = innerIterator.next().trim();
                    	String optionText;
                    	// out.print("Value: " + value + ": OptionValue: " + optionValue + ". Does value.trim().equals(optionValue)? " + value.equals(opti$
                    	if (optionValue.equals(value.trim()))
                    	{	
                    		optionText  = (String) allTasks.get(optionValue);
                            select.addOption(optionValue, optionText);
                        }
                    }
                }
    		}
        }
        return select;
    }
    
}
