/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.Arrays;
import java.util.List;

/**
 * TaskQueueEntry defines the record or entry in the named task queues.
 * Regular immutable value object class.
 * 
 * @author richardrodgers
 */
public final class TaskQueueEntry
{
    private final String epersonId;
    private final String submitTime;
    private final String tasks;
    private final String objId;
    
    /**
     * TaskQueueEntry constructor with enumerated field values.
     * 
     * @param epersonId
     * @param submitTime
     * @param taskNames
     * @param objId
     */
    public TaskQueueEntry(String epersonId, long submitTime,
                          List<String> taskNames, String objId)
    {
        this.epersonId = epersonId;
        this.submitTime = Long.toString(submitTime);
        StringBuilder sb = new StringBuilder();
        for (String tName : taskNames)
        {
            sb.append(tName).append(",");
        }
        this.tasks = sb.substring(0, sb.length() - 1);
        this.objId = objId;
    }
    
    /**
     * Constructor with a pipe-separated list of field values.
     * 
     * @param entry
     *        list of field values separated by '|'s
     */
    public TaskQueueEntry(String entry)
    {
        String[] tokens = entry.split("\\|");
        epersonId = tokens[0];
        submitTime = tokens[1];
        tasks = tokens[2];
        objId = tokens[3];
    }
    
    /**
     * Returns the epersonId (email) of the agent who enqueued this task entry.
     *  
     * @return epersonId
     *         name of EPerson (email) or 'unknown' if none recorded.
     */
    public String getEpersonId()
    {
        return epersonId;
    }
    
    /**
     * Returns the timestamp of when this entry was enqueued.
     * 
     * @return time
     *         Submission timestamp
     */
    public long getSubmitTime()
    {
        return Long.valueOf(submitTime);
    }
    
    /**
     * Return the list of tasks associated with this entry.
     * 
     * @return tasks
     *         the list of task names (Plugin names)
     */
    public List<String> getTaskNames()
    {
        return Arrays.asList(tasks.split(","));
    }
    
    /**
     * Returns the object identifier.
     * @return objId
     *         usually a handle or workflow id
     */
    public String getObjectId()
    {
        return objId;
    }
    /**
     * Returns a string representation of the entry
     * @return string
     *         pipe-separated field values
     */
    @Override
    public String toString()
    {
        return epersonId + "|" + submitTime + "|" + tasks + "|" + objId;
    }
}
