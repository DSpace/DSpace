/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.NullOutputStream;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.factory.CurateServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * CurationCli provides command-line access to Curation tools and processes.
 *
 * @author richardrodgers
 */
public class CurationCli extends DSpaceRunnable<CurationScriptConfiguration> {

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private Context context;
    private CurationClientOptions curationClientOptions;

    private String scope;
    private String reporter;
    private Map<String, String> parameters;
    private boolean verbose;

    @Override
    public void internalRun() throws Exception {
        if (curationClientOptions == CurationClientOptions.HELP) {
            printHelp();
            return;
        }

        if (!this.initLineOptionsAndCheckIfValid()) {
            return;
        }

        Curator curator = initCurator();

        // load curation tasks
        if (curationClientOptions == CurationClientOptions.TASK) {
            long start = System.currentTimeMillis();
            if (!handleCurationTaskAndReturnSuccess(curator)) {
                return;
            }
            this.endScript(start);
        }

        // process task queue
        if (curationClientOptions == CurationClientOptions.QUEUE) {
            // process the task queue
            TaskQueue queue = (TaskQueue) CoreServiceFactory.getInstance().getPluginService()
                                                            .getSinglePlugin(TaskQueue.class);
            if (queue == null) {
                super.handler.logError("No implementation configured for queue");
                throw new UnsupportedOperationException("No queue service available");
            }
            long timeRun = this.runQueue(queue, curator);
            this.endScript(timeRun);
        }
    }

    /**
     * Does the curation task (-t) or the task in the given file (-T).
     * Returns false if required option -i is missing.
     *
     * @return False if there are missing/invalid command line options (-i); otherwise true
     */
    private boolean handleCurationTaskAndReturnSuccess(Curator curator) throws IOException, SQLException {
        if (!commandLine.hasOption('i')) {
            super.handler.logWarning("Id must be specified: a handle, 'all', or no -i and a -T task queue (-h for " +
                                     "help)");
            return false;
        }
        String idName = this.commandLine.getOptionValue('i');
        String taskName;
        if (commandLine.hasOption('t')) {
            taskName = commandLine.getOptionValue('t');
            if (verbose) {
                handler.logInfo("Adding task: " + taskName);
            }
            curator.addTask(taskName);
            if (verbose && !curator.hasTask(taskName)) {
                handler.logInfo("Task: " + taskName + " not resolved");
            }
        } else if (commandLine.hasOption('T')) {
            // load taskFile
            String taskFileName = commandLine.getOptionValue('T');
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(taskFileName));
                while ((taskName = reader.readLine()) != null) {
                    if (verbose) {
                        super.handler.logInfo("Adding task: " + taskName);
                    }
                    curator.addTask(taskName);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        // run tasks against object
        if (verbose) {
            super.handler.logInfo("Starting curation");
            super.handler.logInfo("Curating id: " + idName);
        }
        if ("all".equals(idName)) {
            // run on whole Site
            curator.curate(context,
                ContentServiceFactory.getInstance().getSiteService().findSite(context).getHandle());
        } else {
            curator.curate(context, idName);
        }
        return true;
    }

    /**
     * Runs task queue (-q set)
     *
     * @param queue   The task queue
     * @param curator The curator
     * @return Time when queue started
     */
    private long runQueue(TaskQueue queue, Curator curator) throws SQLException, AuthorizeException, IOException {
        String taskQueueName = this.commandLine.getOptionValue('q');
        // use current time as our reader 'ticket'
        long ticket = System.currentTimeMillis();
        Iterator<TaskQueueEntry> entryIter = queue.dequeue(taskQueueName, ticket).iterator();
        while (entryIter.hasNext()) {
            TaskQueueEntry entry = entryIter.next();
            if (verbose) {
                super.handler.logInfo("Curating id: " + entry.getObjectId());
            }
            curator.clear();
            // does entry relate to a DSO or workflow object?
            if (entry.getObjectId().indexOf('/') > 0) {
                for (String task : entry.getTaskNames()) {
                    curator.addTask(task);
                }
                curator.curate(context, entry.getObjectId());
            } else {
                // make eperson who queued task the effective user
                EPerson agent = ePersonService.findByEmail(context, entry.getEpersonId());
                if (agent != null) {
                    context.setCurrentUser(agent);
                }
                CurateServiceFactory.getInstance().getWorkflowCuratorService()
                                    .curate(curator, context, entry.getObjectId());
            }
        }
        queue.release(taskQueueName, ticket, true);
        return ticket;
    }

    /**
     * End of curation script; logs script time if -v verbose is set
     *
     * @param timeRun Time script was started
     * @throws SQLException If DSpace contextx can't complete
     */
    private void endScript(long timeRun) throws SQLException {
        context.complete();
        if (verbose) {
            long elapsed = System.currentTimeMillis() - timeRun;
            this.handler.logInfo("Ending curation. Elapsed time: " + elapsed);
        }
    }

    /**
     * Fills in some optional command line options; to be used by curator. And check if no required options are
     * missing; or if they have an invalid value. If they are missing/invalid it returns false; otherwise true
     *
     * @return False if there are missing required options or invalid values for options.
     */
    private boolean initLineOptionsAndCheckIfValid() {
        // report file
        if (this.commandLine.hasOption('r')) {
            this.reporter = this.commandLine.getOptionValue('r');
        }

        // parameters
        this.parameters = new HashMap<>();
        if (this.commandLine.hasOption('p')) {
            for (String parameter : this.commandLine.getOptionValues('p')) {
                String[] parts = parameter.split("=", 2);
                String name = parts[0].trim();
                String value;
                if (parts.length > 1) {
                    value = parts[1].trim();
                } else {
                    value = "true";
                }
                this.parameters.put(name, value);
            }
        }

        // verbose
        verbose = false;
        if (commandLine.hasOption('v')) {
            verbose = true;
        }

        // scope
        if (this.commandLine.getOptionValue('s') != null) {
            this.scope = this.commandLine.getOptionValue('s');
            if (this.scope != null && Curator.TxScope.valueOf(this.scope.toUpperCase()) == null) {
                this.handler.logError("Bad transaction scope '" + this.scope + "': only 'object', 'curation' or " +
                                      "'open' recognized");
                return false;
            }
        }

        return true;
    }

    /**
     * Initialize the curator with command line variables
     *
     * @return Initialised curator
     * @throws FileNotFoundException If file of command line variable -r reporter is not found
     */
    private Curator initCurator() throws FileNotFoundException {
        Curator curator = new Curator();
        OutputStream reporterStream;
        if (null == this.reporter) {
            reporterStream = new NullOutputStream();
        } else if ("-".equals(this.reporter)) {
            reporterStream = System.out;
        } else {
            reporterStream = new PrintStream(this.reporter);
        }
        Writer reportWriter = new OutputStreamWriter(reporterStream);
        curator.setReporter(reportWriter);

        if (this.scope != null) {
            Curator.TxScope txScope = Curator.TxScope.valueOf(this.scope.toUpperCase());
            curator.setTransactionScope(txScope);
        }

        curator.addParameters(parameters);
        // we are operating in batch mode, if anyone cares.
        curator.setInvoked(Curator.Invoked.BATCH);
        return curator;
    }

    @Override
    public void printHelp() {
        super.printHelp();
        super.handler.logInfo("\nwhole repo: CurationCli -t estimate -i all");
        super.handler.logInfo("single item: CurationCli -t generate -i itemId");
        super.handler.logInfo("task queue: CurationCli -q monthly");
    }

    @Override
    public CurationScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("curate", CurationScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        try {
            this.context = new Context(Context.Mode.BATCH_EDIT);
            if (this.commandLine.hasOption('e')) {
                String ePersonEmail = this.commandLine.getOptionValue('e');
                EPerson ePerson = ePersonService.findByEmail(this.context, ePersonEmail);
                if (ePerson == null) {
                    super.handler.logError("EPerson not found: " + ePersonEmail);
                    throw new ParseException("EPerson not found: " + ePersonEmail);
                }
                this.context.setCurrentUser(ePerson);
            } else {
                throw new IllegalArgumentException("Needs an -e to set eperson (admin)");
            }
        } catch (Exception e) {
            throw new ParseException("Unable to create a new DSpace Context: " + e.getMessage());
        }
        this.curationClientOptions = CurationClientOptions.getClientOption(commandLine);
    }
}
