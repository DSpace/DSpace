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
    
    protected static FlowResult getRunFlowResult(String taskName, Curator curator)
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
        flowResult.setOutcome(true);
        flowResult.setMessage(new Message("default","The task, " + taskName +
                " was completed with the status: " + status + ".\n" +
                "Results: " + "\n" +
                ((result != null) ? result : "Nothing to do for this DSpace object.")));
        flowResult.setContinue(true);
        return flowResult;
    }
    
    protected static FlowResult getQueueFlowResult(String taskName, boolean status,
                                                   String objId, String queueName)
    {
        FlowResult flowResult = new FlowResult();
        flowResult.setOutcome(true);
        flowResult.setMessage(new Message("default", " The task, " + taskName + ", has " +
                      ((status) ? "been queued for id, " + objId + " in the " + queueName + " queue.": 
                        "has not been queued for id, " + objId + ". An error occurred.")));
        flowResult.setContinue(true);
        return flowResult;
    }
	
}
