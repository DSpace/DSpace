/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * FileTaskQueue provides a TaskQueue implementation based on flat files
 * for the queues and semaphores. 
 *
 * @author richardrodgers
 */
public class FileTaskQueue implements TaskQueue
{
    private static Logger log = Logger.getLogger(TaskQueue.class);   
    // base directory for curation task queues
    protected String tqDir;

    // ticket for queue readers
    protected long readTicket = -1L;
    // list of queues owned by reader
    protected List<Integer> readList = new ArrayList<Integer>();

    public FileTaskQueue()
    {
        tqDir = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("curate.taskqueue.dir");
    }
    
    @Override
    public String[] queueNames()
    {
        return new File(tqDir).list();
    }
    
    @Override
    public synchronized void enqueue(String queueName, TaskQueueEntry entry)
           throws IOException
    {
        Set entrySet = new HashSet<TaskQueueEntry>();
        entrySet.add(entry);
        enqueue(queueName, entrySet);
    }

    @Override
    public synchronized void enqueue(String queueName, Set<TaskQueueEntry> entrySet)
           throws IOException
    {
        // don't block or fail - iterate until an unlocked queue found/created
        int queueIdx = 0;
        File qDir = ensureQueue(queueName);
        while (true)
        {
            File lock = new File(qDir, "lock" + Integer.toString(queueIdx));

            // Check for lock, and create one if it doesn't exist.
            // If the lock file already exists, this will return false
            if (lock.createNewFile())
            {
                // append set contents to queue
                BufferedWriter writer = null;
                try
                {
                    File queue = new File(qDir, "queue" + Integer.toString(queueIdx));
                    writer = new BufferedWriter(new FileWriter(queue, true));
                    Iterator<TaskQueueEntry> iter = entrySet.iterator();
                    while (iter.hasNext())
                    {
                        writer.write(iter.next().toString());
                        writer.newLine();
                    }
                }
                finally
                {
                    if (writer != null)
                    {
                        writer.close();
                    }
                }
                // remove lock
                if (!lock.delete())
                {
                    log.error("Unable to remove lock: " + lock.getName());
                }
                break;
            }
            queueIdx++;
        }
    }

    @Override
    public synchronized Set<TaskQueueEntry> dequeue(String queueName, long ticket)
           throws IOException
    {
        Set<TaskQueueEntry> entrySet = new HashSet<TaskQueueEntry>();
        if (readTicket == -1L)
        {
            // hold the ticket & copy all Ids available, locking queues
            // stop when no more queues or one found locked
            File qDir = ensureQueue(queueName);
            readTicket = ticket;
            int queueIdx = 0;
            while (true)
            {
                File queue = new File(qDir, "queue" + Integer.toString(queueIdx));
                File lock = new File(qDir, "lock" + Integer.toString(queueIdx));

                // If the queue file exists, atomically check for a lock file and create one if it doesn't exist
                // If the lock file exists already, then this simply returns false
                if (queue.exists() && lock.createNewFile()) {
                    // read contents from file
                    BufferedReader reader = null;
                    try
                    {
                        reader = new BufferedReader(new FileReader(queue));
                        String entryStr = null;
                        while ((entryStr = reader.readLine()) != null)
                        {
                            entryStr = entryStr.trim();
                            if (entryStr.length() > 0)
                            {
                                entrySet.add(new TaskQueueEntry(entryStr));
                            }
                        }
                    }
                    finally
                    {
                        if (reader != null)
                        {
                            reader.close();    
                        }
                    }
                    readList.add(queueIdx);
                }
                else
                {
                    break;
                }
                queueIdx++;
            }
        }
        return entrySet;
    }
    
    @Override
    public synchronized void release(String queueName, long ticket, boolean remove)
    {
        if (ticket == readTicket)
        {
            readTicket = -1L;
            File qDir = ensureQueue(queueName);
            // remove locks & queues (if flag true)
            for (Integer queueIdx : readList)
            {
                File lock = new File(qDir, "lock" + Integer.toString(queueIdx));
                if (remove)
                {
                    File queue = new File(qDir, "queue" + Integer.toString(queueIdx));
                    if (!queue.delete())
                    {
                        log.error("Unable to delete queue file: " + queue.getName());
                    }
                }

                if (!lock.delete())
                {
                    log.error("Unable to delete lock file: " + lock.getName());
                }
            }
            readList.clear();
        }
    }
    
    protected File ensureQueue(String queueName)
    {
        // create directory structures as needed
        File baseDir = new File(tqDir, queueName);
        if (!baseDir.exists() && !baseDir.mkdirs())
        {
            throw new IllegalStateException("Unable to create directories");
        }

        return baseDir;
    }
}
