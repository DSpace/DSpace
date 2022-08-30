/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.NullOutputStream;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * CurationCli provides command-line access to Curation tools and processes.
 *
 * @author richardrodgers
 */
public class Curation extends DSpaceRunnable<CurationScriptConfiguration> {

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    protected Context context;
    private CurationClientOptions curationClientOptions;

    private String task;
    private String taskFile;
    private String id;
    private String queue;
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

        Curator curator = initCurator();

        // load curation tasks
        if (curationClientOptions == CurationClientOptions.TASK) {
            long start = System.currentTimeMillis();
            handleCurationTask(curator);
            this.endScript(start);
        }

        // process task queue
        if (curationClientOptions == CurationClientOptions.QUEUE) {
            // process the task queue
            TaskQueue taskQueue = (TaskQueue) CoreServiceFactory.getInstance().getPluginService()
                                                            .getSinglePlugin(TaskQueue.class);
            if (taskQueue == null) {
                super.handler.logError("No implementation configured for queue");
                throw new UnsupportedOperationException("No queue service available");
            }
            long timeRun = this.runQueue(taskQueue, curator);
            this.endScript(timeRun);
        }
    }

    /**
     * Does the curation task (-t) or the task in the given file (-T).
     * Checks:
     * - if required option -i is missing.
     * - if option -t has a valid task option
     */
    private void handleCurationTask(Curator curator) throws IOException, SQLException {
        String taskName;
        if (commandLine.hasOption('t')) {
            if (verbose) {
                handler.logInfo("Adding task: " + this.task);
            }
            curator.addTask(this.task);
            if (verbose && !curator.hasTask(this.task)) {
                handler.logInfo("Task: " + this.task + " not resolved");
            }
        } else if (commandLine.hasOption('T')) {
            // load taskFile
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(this.taskFile));
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
            super.handler.logInfo("Curating id: " + this.id);
        }
        if ("all".equals(this.id)) {
            // run on whole Site
            curator.curate(context,
                ContentServiceFactory.getInstance().getSiteService().findSite(context).getHandle());
        } else {
            curator.curate(context, this.id);
        }
    }

    /**
     * Runs task queue (-q set)
     *
     * @param queue   The task queue
     * @param curator The curator
     * @return Time when queue started
     */
    private long runQueue(TaskQueue queue, Curator curator) throws SQLException, AuthorizeException, IOException {
        // use current time as our reader 'ticket'
        long ticket = System.currentTimeMillis();
        Iterator<TaskQueueEntry> entryIter = queue.dequeue(this.queue, ticket).iterator();
        while (entryIter.hasNext()) {
            TaskQueueEntry entry = entryIter.next();
            if (verbose) {
                super.handler.logInfo("Curating id: " + entry.getObjectId());
            }
            curator.clear();
            // does entry relate to a DSO or workflow object?
            if (entry.getObjectId().indexOf('/') > 0) {
                for (String taskName : entry.getTaskNames()) {
                    curator.addTask(taskName);
                }
                curator.curate(context, entry.getObjectId());
            } else {
                // TODO: Remove this exception once curation tasks are supported by configurable workflow
                // e.g. see https://github.com/DSpace/DSpace/pull/3157
                throw new IllegalArgumentException("curation for workflow items is no longer supported");
            }
        }
        queue.release(this.queue, ticket, true);
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
        assignCurrentUserInContext();
        this.curationClientOptions = CurationClientOptions.getClientOption(commandLine);

        if (this.curationClientOptions != null) {
            this.initGeneralLineOptionsAndCheckIfValid();
            if (curationClientOptions == CurationClientOptions.TASK) {
                this.initTaskLineOptionsAndCheckIfValid();
            } else if (curationClientOptions == CurationClientOptions.QUEUE) {
                this.queue = this.commandLine.getOptionValue('q');
            }
        } else {
            throw new IllegalArgumentException("[--help || --task|--taskfile <> -identifier <> || -queue <> ] must be" +
                                               " specified");
        }
    }

    /**
     * This method will assign the currentUser to the {@link Context} variable which is also created in this method.
     * The instance of the method in this class will fetch the EPersonIdentifier from this class, this identifier
     * was given to this class upon instantiation, it'll then be used to find the {@link EPerson} associated with it
     * and this {@link EPerson} will be set as the currentUser of the created {@link Context}
     * @throws ParseException If something went wrong with the retrieval of the EPerson Identifier
     */
    protected void assignCurrentUserInContext() throws ParseException {
        UUID currentUserUuid = this.getEpersonIdentifier();
        try {
            this.context = new Context(Context.Mode.BATCH_EDIT);
            EPerson eperson = ePersonService.find(context, currentUserUuid);
            if (eperson == null) {
                super.handler.logError("EPerson not found: " + currentUserUuid);
                throw new IllegalArgumentException("Unable to find a user with uuid: " + currentUserUuid);
            }
            assignSpecialGroupsInContext();
            this.context.setCurrentUser(eperson);
        } catch (SQLException e) {
            handler.handleException("Something went wrong trying to fetch eperson for uuid: " + currentUserUuid, e);
        }
    }

    protected void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    /**
     * Fills in some optional command line options.
     * Checks if there are missing required options or invalid values for options.
     */
    private void initGeneralLineOptionsAndCheckIfValid() {
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
                throw new IllegalArgumentException(
                    "Bad transaction scope '" + this.scope + "': only 'object', 'curation' or " +
                    "'open' recognized");
            }
        }
    }

    /**
     * Fills in required command line options for the task or taskFile option.
     * Checks if there are is a missing required -i option and if -i is either 'all' or a valid dso handle.
     * Checks if -t task has a valid task option.
     * Checks if -T taskfile is a valid file.
     */
    private void initTaskLineOptionsAndCheckIfValid() {
        // task or taskFile
        if (this.commandLine.hasOption('t')) {
            this.task = this.commandLine.getOptionValue('t');
            if (!CurationClientOptions.getTaskOptions().contains(this.task)) {
                super.handler
                    .logError("-t task must be one of: " + CurationClientOptions.getTaskOptions());
                throw new IllegalArgumentException(
                    "-t task must be one of: " + CurationClientOptions.getTaskOptions());
            }
        } else if (this.commandLine.hasOption('T')) {
            this.taskFile = this.commandLine.getOptionValue('T');
            if (!(new File(this.taskFile).isFile())) {
                super.handler
                    .logError("-T taskFile must be valid file: " + this.taskFile);
                throw new IllegalArgumentException("-T taskFile must be valid file: " + this.taskFile);
            }
        }

        if (this.commandLine.hasOption('i')) {
            this.id = this.commandLine.getOptionValue('i').toLowerCase();
            if (!this.id.equalsIgnoreCase("all")) {
                HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
                DSpaceObject dso;
                try {
                    dso = handleService.resolveToObject(this.context, id);
                } catch (SQLException e) {
                    super.handler.logError("SQLException trying to resolve handle " + id + " to a valid dso");
                    throw new IllegalArgumentException(
                        "SQLException trying to resolve handle " + id + " to a valid dso");
                }
                if (dso == null) {
                    super.handler.logError("Id must be specified: a valid dso handle or 'all'; " + this.id + " could " +
                                           "not be resolved to valid dso handle");
                    throw new IllegalArgumentException(
                        "Id must be specified: a valid dso handle or 'all'; " + this.id + " could " +
                        "not be resolved to valid dso handle");
                }
            }
        } else {
            super.handler.logError("Id must be specified: a handle, 'all', or no -i and a -q task queue (-h for " +
                                   "help)");
            throw new IllegalArgumentException(
                "Id must be specified: a handle, 'all', or no -i and a -q task queue (-h for " +
                "help)");
        }
    }
}
