/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

<<<<<<< HEAD
import org.apache.commons.cli.*;
import org.dspace.content.factory.ContentServiceFactory;
=======
import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
>>>>>>> dspace-7.2.1
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

/**
 * This is the CLI version of the {@link Curation} script.
 * This will only be called when the curate script is called from a commandline instance.
 */
<<<<<<< HEAD
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

        Context c = new Context(Context.Mode.BATCH_EDIT);
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
=======
public class CurationCli extends Curation {

    /**
     * This is the overridden instance of the {@link Curation#assignCurrentUserInContext()} method in the parent class
     * {@link Curation}.
     * This is done so that the CLI version of the Script is able to retrieve its currentUser from the -e flag given
     * with the parameters of the Script.
     * @throws ParseException   If the e flag was not given to the parameters when calling the script
     */
    @Override
    protected void assignCurrentUserInContext() throws ParseException {
        if (this.commandLine.hasOption('e')) {
            String ePersonEmail = this.commandLine.getOptionValue('e');
            this.context = new Context(Context.Mode.BATCH_EDIT);
            try {
                EPerson ePerson = ePersonService.findByEmail(this.context, ePersonEmail);
                if (ePerson == null) {
                    super.handler.logError("EPerson not found: " + ePersonEmail);
                    throw new IllegalArgumentException("Unable to find a user with email: " + ePersonEmail);
>>>>>>> dspace-7.2.1
                }
                this.context.setCurrentUser(ePerson);
            } catch (SQLException e) {
                throw new IllegalArgumentException("SQLException trying to find user with email: " + ePersonEmail);
            }
        } else {
            throw new ParseException("Required parameter -e missing!");
        }
    }
}
