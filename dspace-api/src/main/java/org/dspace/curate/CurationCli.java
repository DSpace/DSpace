/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.factory.CurateServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * CurationCli provides command-line access to Curation tools and processes.
 * 
 * @author richardrodgers
 */
public class CurationCli
{    
    public static void main(String[] args) throws Exception
    {
         // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("t", "task", true,
                "curation task name");
        options.addOption("T", "taskfile", true,
                "file containing curation task names");
        options.addOption("i", "id", true,
                "Id (handle) of object to perform task on, or 'all' to perform on whole repository");
        options.addOption("q", "queue", true,
                 "name of task queue to process");
        options.addOption("e", "eperson", true,
                "email address of curating eperson");
        options.addOption("r", "reporter", true,
                "reporter to manage results - use '-' to report to console. If absent, no reporting");
        options.addOption("s", "scope", true,
                "transaction scope to impose: use 'object', 'curation', or 'open'. If absent, 'open' applies");
        options.addOption("v", "verbose", false,
                "report activity to stdout");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, args);

        String taskName = null;
        String taskFileName = null;
        String idName = null;
        String taskQueueName = null;
        String ePersonName = null;
        String reporterName = null;
        String scope = null;
        boolean verbose = false;

        if (line.hasOption('h'))
        {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("CurationCli\n", options);
            System.out
                    .println("\nwhole repo: CurationCli -t estimate -i all");
            System.out
                    .println("single item: CurationCli -t generate -i itemId");
            System.out
                    .println("task queue: CurationCli -q monthly");
            System.exit(0);
        }

        if (line.hasOption('t'))
        { // task
            taskName = line.getOptionValue('t');
        }

        if (line.hasOption('T'))
        { // task file
            taskFileName = line.getOptionValue('T');
        }

        if (line.hasOption('i'))
        { // id
            idName = line.getOptionValue('i');
        }

        if (line.hasOption('q'))
        { // task queue
            taskQueueName = line.getOptionValue('q');
        }

        if (line.hasOption('e'))
        { // eperson
            ePersonName = line.getOptionValue('e');
        }

        if (line.hasOption('r'))
        { // report file
            reporterName = line.getOptionValue('r');
        }
        

        if (line.hasOption('s'))
        { // transaction scope
            scope = line.getOptionValue('s');
        }

        if (line.hasOption('v'))
        { // verbose
            verbose = true;
        }

        // now validate the args
        if (idName == null && taskQueueName == null)
        {
            System.out.println("Id must be specified: a handle, 'all', or a task queue (-h for help)");
            System.exit(1);
        }

        if (taskName == null && taskFileName == null && taskQueueName == null)
        {
            System.out.println("A curation task or queue must be specified (-h for help)");
            System.exit(1);
        }
        
        if (scope != null && Curator.TxScope.valueOf(scope.toUpperCase()) == null)
    	{
        	System.out.println("Bad transaction scope '" + scope + "': only 'object', 'curation' or 'open' recognized");
        	System.exit(1);
    	}
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Context c = new Context();
        if (ePersonName != null)
        {
            EPerson ePerson = ePersonService.findByEmail(c, ePersonName);
            if (ePerson == null)
            {
                System.out.println("EPerson not found: " + ePersonName);
                System.exit(1);
            }
            c.setCurrentUser(ePerson);
        }
        else
        {
            c.turnOffAuthorisationSystem();
        }

        Curator curator = new Curator();
        if (reporterName != null)
        {
            curator.setReporter(reporterName);
        }
        if (scope != null)
        {
        	Curator.TxScope txScope = Curator.TxScope.valueOf(scope.toUpperCase());
        	curator.setTransactionScope(txScope);
        }
        // we are operating in batch mode, if anyone cares.
        curator.setInvoked(Curator.Invoked.BATCH);
        // load curation tasks
        if (taskName != null)
        {
            if (verbose)
            {
                System.out.println("Adding task: " + taskName);
            }
            curator.addTask(taskName);
            if (verbose && ! curator.hasTask(taskName))
            {
                System.out.println("Task: " + taskName + " not resolved");
            }
        }
        else if (taskQueueName == null)
        {
            // load taskFile
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(taskFileName));
                while ((taskName = reader.readLine()) != null)
                {
                    if (verbose)
                    {
                        System.out.println("Adding task: " + taskName);
                    }
                    curator.addTask(taskName);
                }
            }
            finally
            {
                if (reader != null)
                {
                  reader.close();  
                }
            }
        }
        // run tasks against object
        long start = System.currentTimeMillis();
        if (verbose)
        {
            System.out.println("Starting curation");
        }
        if (idName != null)
        {
            if (verbose)
            {
               System.out.println("Curating id: " + idName);
            }
            if ("all".equals(idName))
            {
            	// run on whole Site
            	curator.curate(c, ContentServiceFactory.getInstance().getSiteService().findSite(c).getHandle());
            }
            else
            {
                curator.curate(c, idName);
            }
        }
        else
        {
            // process the task queue
            TaskQueue queue = (TaskQueue) CoreServiceFactory.getInstance().getPluginService().getSinglePlugin(TaskQueue.class);
            if (queue == null)
            {
                System.out.println("No implementation configured for queue");
                throw new UnsupportedOperationException("No queue service available");
            }
            // use current time as our reader 'ticket'
            long ticket = System.currentTimeMillis();
            Iterator<TaskQueueEntry> entryIter = queue.dequeue(taskQueueName, ticket).iterator();
            while (entryIter.hasNext())
            {
                TaskQueueEntry entry = entryIter.next();
                if (verbose)
                {
                    System.out.println("Curating id: " + entry.getObjectId());
                }
                curator.clear();
                // does entry relate to a DSO or workflow object?
                if (entry.getObjectId().indexOf("/") > 0)
                {
                    for (String task : entry.getTaskNames())
                    {
                        curator.addTask(task);
                    }
                    curator.curate(c, entry.getObjectId());
                }
                else
                {
                    // make eperson who queued task the effective user
                    EPerson agent = ePersonService.findByEmail(c, entry.getEpersonId());
                    if (agent != null)
                    {
                        c.setCurrentUser(agent);
                    }
                    CurateServiceFactory.getInstance().getWorkflowCuratorService().curate(curator, c, entry.getObjectId());
                }
            }
            queue.release(taskQueueName, ticket, true);
        }
        c.complete();
        if (verbose)
        {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Ending curation. Elapsed time: " + elapsed);
        }
    }
}
