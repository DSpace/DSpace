/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.Curator;

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
            String statusCodes = ConfigurationManager.getProperty("curate", "ui.statusmessages");
            for (String pair : statusCodes.split(","))
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
    private static String getUITaskName(String taskID)
    {       
            String tasksString = ConfigurationManager.getProperty("curate", "ui.tasknames");
            String[] tasks = tasksString.split(",");
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
}
