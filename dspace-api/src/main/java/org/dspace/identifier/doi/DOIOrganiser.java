/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.FilterUtils;
import org.dspace.content.logic.TrueFilter;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * @author Marsa Haoua
 * @author Pascal-Nicolas Becker
 * @author Bram Luyten (Atmire)
 */
public class DOIOrganiser {

    private static final Logger LOG = LogManager.getLogger(DOIOrganiser.class);

    private final DOIIdentifierProvider provider;
    private final Context context;
    private boolean quiet;
    protected HandleService handleService;
    protected ItemService itemService;
    protected DOIService doiService;
    protected ConfigurationService configurationService;
    // This filter will override the default provider filter / behaviour
    protected Filter filter;
    private int batchSize = 100; // Default batch size

    private interface DOIOperation {
        void perform(Context context, DOIIdentifierProvider provider, DOI doi) throws SQLException, IdentifierException;
    }

    /**
     * Constructor to be called within the main() method
     * @param context   - DSpace context
     * @param provider  - DOI identifier provider to use
     */
    public DOIOrganiser(Context context, DOIIdentifierProvider provider) {
        this.context = context;
        this.provider = provider;
        this.quiet = false;
        this.handleService = HandleServiceFactory.getInstance().getHandleService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.doiService = IdentifierServiceFactory.getInstance().getDOIService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.filter = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                "always_true_filter", TrueFilter.class);
    }

    /**
     * Set the batch size for processing operations
     * @param batchSize - number of operations after which to commit
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    /**
     * Get the batch size for processing operations
     * @return batchSize
     */
    public int getBatchSize() {
        return this.batchSize;
    }


    /**
     * Main command-line runner method as with other DSpace launcher commands
     * @param args  - the command line arguments to parse as parameters
     */
    public static void main(String[] args) {
        LOG.debug("Starting DOI organiser ");

        // setup Context
        Context context = new Context();

        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        DOIOrganiser organiser = new DOIOrganiser(context,
            new DSpace().getSingletonService(DOIIdentifierProvider.class));
        // run command line interface
        runCLI(context, organiser, args);

        try {
            context.complete();
        } catch (SQLException sqle) {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

    }

    public static void runCLI(Context context, DOIOrganiser organiser, String[] args) {
        // initialize options
        Options options = new Options();

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Help")
                .build());

        options.addOption(Option.builder("l")
                .longOpt("list")
                .desc("List all objects to be reserved, registered, deleted or updated")
                .build());

        options.addOption(Option.builder("r")
                .longOpt("register-all")
                .desc("Perform online registration for all identifiers queued for registration.")
                .build());

        options.addOption(Option.builder("s")
                .longOpt("reserve-all")
                .desc("Perform online reservation for all identifiers queued for reservation.")
                .build());

        options.addOption(Option.builder("u")
                .longOpt("update-all")
                .desc("Perform online metadata update for all identifiers queued for metadata update.")
                .build());

        options.addOption(Option.builder("d")
                .longOpt("delete-all")
                .desc("Perform online deletion for all identifiers queued for deletion.")
                .build());

        options.addOption(Option.builder("q")
                .longOpt("quiet")
                .desc("Turn the command line output off.")
                .build());

        options.addOption(Option.builder()
                .longOpt("filter")
                .hasArg()
                .argName("filterName")
                .desc("Use the specified filter name instead of the provider's filter. " +
                      "Defaults to a special 'always true' filter to force operations")
                .build());

        options.addOption(Option.builder()
                .longOpt("register-doi")
                .hasArg()
                .argName("DOI|ItemID|handle")
                .desc("Register a specified identifier. You can specify the identifier by ItemID, Handle or DOI.")
                .build());

        options.addOption(Option.builder()
                .longOpt("reserve-doi")
                .hasArg()
                .argName("DOI|ItemID|handle")
                .desc("Reserve a specified identifier online. You can specify the identifier by ItemID, Handle or DOI.")
                .build());

        options.addOption(Option.builder()
                .longOpt("update-doi")
                .hasArg()
                .argName("DOI|ItemID|handle")
                .desc("Update online an object for a given DOI identifier or ItemID or Handle.")
                .build());

        options.addOption(Option.builder()
                .longOpt("delete-doi")
                .hasArg()
                .argName("DOI identifier")
                .desc("Delete a specified identifier.")
                .build());

        options.addOption(Option.builder()
                .longOpt("batch-size")
                .hasArg()
                .argName("size")
                .desc("Set the batch size for processing operations. Default is 100.")
                .build());

        // initialize parser
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        HelpFormatter helpformater = new HelpFormatter();

        try {
            line = parser.parse(options, args);
        } catch (ParseException ex) {
            LOG.fatal(ex);
            System.exit(1);
        }

        // process options
        // user asks for help
        if (line.hasOption('h') || 0 == line.getOptions().length) {
            helpformater.printHelp("\nDOI organiser\n", options);
        }

        if (line.hasOption('q')) {
            organiser.setQuiet();
        }

        if (line.hasOption("batch-size")) {
            int batchSize = Integer.parseInt(line.getOptionValue("batch-size"));
            organiser.setBatchSize(batchSize);
        }

        if (line.hasOption('l')) {
            organiser.list("reservation", null, null, DOIIdentifierProvider.TO_BE_RESERVED);
            organiser.list("registration", null, null, DOIIdentifierProvider.TO_BE_REGISTERED);
            organiser.list("update", null, null,
                           DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION,
                           DOIIdentifierProvider.UPDATE_REGISTERED,
                           DOIIdentifierProvider.UPDATE_RESERVED);
            organiser.list("deletion", null, null, DOIIdentifierProvider.TO_BE_DELETED);
        }

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
        // Do we get a filter?
        if (line.hasOption("filter")) {
            String filter = line.getOptionValue("filter");
            if (null != filter) {
                organiser.filter = FilterUtils.getFilterFromConfiguration(filter);
            }
        }

        if (line.hasOption('s')) {
            try {
                List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(DOIIdentifierProvider.TO_BE_RESERVED));
                if (dois.isEmpty()) {
                    System.err.println("There are no objects in the database that could be reserved.");
                } else {
                    organiser.reserve(dois);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption('r')) {
            try {
                List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(DOIIdentifierProvider.TO_BE_REGISTERED));
                if (dois.isEmpty()) {
                    System.err.println("There are no objects in the database that could be registered.");
                } else {
                    organiser.register(dois);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption('u')) {
            try {
                List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(
                    DOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION,
                    DOIIdentifierProvider.UPDATE_RESERVED,
                    DOIIdentifierProvider.UPDATE_REGISTERED));
                if (dois.isEmpty()) {
                    System.err.println("There are no objects in the database whose metadata needs an update.");
                } else {
                    organiser.update(dois);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption('d')) {
            try {
                List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(DOIIdentifierProvider.TO_BE_DELETED));
                if (dois.isEmpty()) {
                    System.err.println("There are no objects in the database that could be deleted.");
                } else {
                    organiser.delete(dois);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption("reserve-doi")) {
            String identifier = line.getOptionValue("reserve-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    DOI doiRow = organiser.resolveToDOI(identifier);
                    organiser.reserve(doiRow);
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        if (line.hasOption("register-doi")) {
            String identifier = line.getOptionValue("register-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    DOI doiRow = organiser.resolveToDOI(identifier);
                    organiser.register(doiRow);
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        if (line.hasOption("update-doi")) {
            String identifier = line.getOptionValue("update-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    DOI doiRow = organiser.resolveToDOI(identifier);
                    organiser.update(doiRow);
                } catch (SQLException | IllegalArgumentException | IllegalStateException | IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }

        if (line.hasOption("delete-doi")) {
            String identifier = line.getOptionValue("delete-doi");

            if (null == identifier) {
                helpformater.printHelp("\nDOI organiser\n", options);
            } else {
                try {
                    organiser.delete(identifier);
                } catch (SQLException | IllegalArgumentException ex) {
                    LOG.error(ex);
                }
            }
        }

    }

    /**
     * list DOIs queued for reservation or registration
     * @param processName   - process name for display
     * @param out           - output stream (eg. STDOUT)
     * @param err           - error output stream (eg. STDERR)
     * @param status        - status codes
     */
    public void list(String processName, PrintStream out, PrintStream err, Integer ... status) {
        String indent = "    ";
        if (null == out) {
            out = System.out;
        }
        if (null == err) {
            err = System.err;
        }

        try {
            List<DOI> doiList = doiService.getDOIsByStatus(context, Arrays.asList(status));
            if (0 < doiList.size()) {
                out.println("DOIs queued for " + processName + ": ");
            } else {
                out.println("There are no DOIs queued for " + processName + ".");
            }
            for (DOI doiRow : doiList) {
                out.print(indent + DOI.SCHEME + doiRow.getDoi());
                DSpaceObject dso = doiRow.getDSpaceObject();
                if (null != dso) {
                    out.println(" (belongs to item with handle " + dso.getHandle() + ")");
                } else {
                    out.println(" (cannot determine handle of assigned object)");
                }
            }
            out.println("");
        } catch (SQLException ex) {
            err.println("Error in database Connection: " + ex.getMessage());
            ex.printStackTrace(err);
        }
    }

    private void performDOIOperation(DOI doiRow, DOIOperation operation, String operationName) {
        DSpaceObject dso = doiRow.getDSpaceObject();
        if (Constants.ITEM != dso.getType()) {
            throw new IllegalArgumentException("Currently DSpace supports DOIs for Items only.");
        }

        try {
            operation.perform(context, provider, doiRow);

            if (!quiet) {
                System.out.println("Successfully " + operationName + " DOI " + DOI.SCHEME + doiRow.getDoi() + ".");
            }
        } catch (IdentifierException ex) {
            handleIdentifierException(ex, operationName, dso, doiRow.getDoi());
        } catch (IllegalArgumentException ex) {
            handleIllegalArgumentException(ex, operationName, doiRow.getDoi());
        } catch (SQLException ex) {
            handleSQLException(ex, operationName, doiRow.getDoi());
        }
    }

    private void handleIdentifierException(IdentifierException ex, String operationName, DSpaceObject dso, String doi) {
        if (!(ex instanceof DOIIdentifierException)) {
            LOG.error("It wasn't possible to " + operationName + " the identifier online. ", ex);
        }

        DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;

        try {
            sendAlertMail(operationName, dso, DOI.SCHEME + doi,
                          doiIdentifierException.codeToString(doiIdentifierException.getCode()));
        } catch (IOException ioe) {
            LOG.error("Couldn't send mail", ioe);
        }

        LOG.error("It wasn't possible to " + operationName + " this identifier: " + DOI.SCHEME + doi
                      + " Exceptions code: " + doiIdentifierException.codeToString(doiIdentifierException.getCode()), ex);

        if (!quiet) {
            System.err.println("It wasn't possible to " + operationName + " this identifier: " + DOI.SCHEME + doi);
        }
    }

    private void handleIllegalArgumentException(IllegalArgumentException ex, String operationName, String doi) {
        LOG.error("Database table DOI contains a DOI that is not valid: " + DOI.SCHEME + doi + "!", ex);

        if (!quiet) {
            System.err.println("It wasn't possible to " + operationName + " this identifier: " + DOI.SCHEME + doi);
        }

        throw new IllegalStateException("Database table DOI contains a DOI that is not valid: " + DOI.SCHEME + doi + "!", ex);
    }

    private void handleSQLException(SQLException ex, String operationName, String doi) {
        LOG.error("Error while trying to " + operationName + " DOI", ex);

        if (!quiet) {
            System.err.println("It wasn't possible to " + operationName + " this identifier: " + DOI.SCHEME + doi);
        }
        throw new RuntimeException("Error while trying to " + operationName + " DOI", ex);
    }

    public void register(DOI doiRow) throws SQLException, DOIIdentifierException {
        performDOIOperation(doiRow, 
            (context, provider, doi) -> provider.registerOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi(), this.filter),
            "register");
    }

    public void reserve(DOI doiRow) {
        performDOIOperation(doiRow,
            (context, provider, doi) -> provider.reserveOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi(), this.filter),
            "reserve");
    }

    public void update(DOI doiRow) {
        performDOIOperation(doiRow,
            (context, provider, doi) -> provider.updateMetadataOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi()),
            "update");
    }

    public void delete(String identifier) throws SQLException {
        String doi = null;
        DOI doiRow = null;

        try {
            doi = doiService.formatIdentifier(identifier);
            doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));

            if (null == doiRow) {
                throw new IllegalStateException("You specified a valid DOI, that is not stored in our database.");
            }

            performDOIOperation(doiRow,
                (context, provider, d) -> provider.deleteOnline(context, DOI.SCHEME + d.getDoi()),
                "delete");

        } catch (DOIIdentifierException ex) {
            LOG.error("It wasn't possible to detect this identifier: " + identifier
                          + " Exceptions code: " + ex.codeToString(ex.getCode()), ex);

            if (!quiet) {
                System.err.println("It wasn't possible to detect this identifier: " + identifier);
            }
        }
    }

    /**
     * Finds the TableRow in the Doi table that belongs to the specified
     * DspaceObject.
     *
     * @param identifier Either an ItemID, a DOI or a handle. If the identifier
     *                   contains digits only we treat it as ItemID, if not we try to find a
     *                   matching doi or a handle (in this order).
     * @return The TableRow or null if the Object does not have a DOI.
     * @throws SQLException             if database error
     * @throws IllegalArgumentException If the identifier is null, an empty
     *                                  String or specifies an DSpaceObject that is not an item. We currently
     *                                  support DOIs for items only, but this may change once...
     * @throws IllegalStateException    If the identifier was a valid DOI that is
     *                                  not stored in our database or if it is a handle that is not bound to an
     *                                  DSpaceObject.
     * @throws IdentifierException      if identifier error
     */
    public DOI resolveToDOI(String identifier)
        throws SQLException, IllegalArgumentException, IllegalStateException, IdentifierException {
        if (null == identifier || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier is null or empty.");
        }

        DOI doiRow = null;
        String doi = null;

        // detect it identifer is ItemID, handle or DOI.
        // try to detect ItemID
        if (identifier
            .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")) {
            DSpaceObject dso = itemService.find(context, UUID.fromString(identifier));

            if (null != dso) {
                doiRow = doiService.findDOIByDSpaceObject(context, dso);

                //Check if this Item has an Identifier, mint one if it doesn't
                if (null == doiRow) {
                    doi = provider.mint(context, dso, this.filter);
                    doiRow = doiService.findByDoi(context,
                                                  doi.substring(DOI.SCHEME.length()));
                    return doiRow;
                }
                return doiRow;
            } else {
                throw new IllegalStateException("You specified an ItemID, that is not stored in our database.");
            }
        }

        // detect handle
        DSpaceObject dso = handleService.resolveToObject(context, identifier);

        if (null != dso) {
            if (dso.getType() != Constants.ITEM) {
                throw new IllegalArgumentException(
                    "Currently DSpace supports DOIs for Items only. "
                        + "Cannot process specified handle as it does not identify an Item.");
            }

            doiRow = doiService.findDOIByDSpaceObject(context, dso);

            if (null == doiRow) {
                doi = provider.mint(context, dso, this.filter);
                doiRow = doiService.findByDoi(context,
                                              doi.substring(DOI.SCHEME.length()));
            }
            return doiRow;
        }
        // detect DOI
        try {
            doi = doiService.formatIdentifier(identifier);
            // If there's no exception: we found a valid DOI. :)
            doiRow = doiService.findByDoi(context,
                                          doi.substring(DOI.SCHEME.length()));
            if (null == doiRow) {
                throw new IllegalStateException("You specified a valid DOI, that is not stored in our database.");
            }
        } catch (DOIIdentifierException ex) {
            // Identifier was not recognized as DOI.
            LOG.error("It wasn't possible to detect this identifier:  "
                          + identifier
                          + " Exceptions code:  "
                          + ex.codeToString(ex.getCode()), ex);

            if (!quiet) {
                System.err.println("It wasn't possible to detect this DOI identifier: "
                                       + identifier);
            }
        }

        return doiRow;
    }

    /**
     * Send an alert email to the configured recipient when DOI operations encounter an error
     * @param action    - action being attempted (eg. reserve, register, update)
     * @param dso       - DSpaceObject associated with the DOI
     * @param doi       - DOI for this operation
     * @param reason    - failure reason or error message
     * @throws IOException
     */
    private void sendAlertMail(String action, DSpaceObject dso, String doi, String reason)
        throws IOException {
        String recipient = configurationService.getProperty("alert.recipient");

        try {
            if (recipient != null) {
                Email email = Email.getEmail(
                    I18nUtil.getEmailFilename(Locale.getDefault(), "doi_maintenance_error"));
                email.addRecipient(recipient);
                email.addArgument(action);
                email.addArgument(new Date());
                email.addArgument(ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getTypeText(dso));
                email.addArgument(dso.getID().toString());
                email.addArgument(doi);
                email.addArgument(reason);
                email.send();

                if (!quiet) {
                    System.err.println("Email alert is sent.");
                }
            }
        } catch (IOException | MessagingException e) {
            LOG.warn("Unable to send email alert", e);
            if (!quiet) {
                System.err.println("Unable to send email alert.");
            }
        }
    }

    /**
     * Set this runner to be in quiet mode, suppressing console output
     */
    private void setQuiet() {
        this.quiet = true;
    }

    private void batchProcess(List<DOI> doiList, DOIOperation operation, String operationName) {
        int batchSize = getBatchSize();
        int count = 0;

        try {
            for (DOI doiRow : doiList) {
                DSpaceObject dso = doiRow.getDSpaceObject();
                if (Constants.ITEM != dso.getType()) {
                    LOG.warn("Skipping DOI " + doiRow.getDoi() + ": Currently DSpace supports DOIs for Items only.");
                    continue;
                }

                try {
                    // Reload the DSpaceObject to ensure it's attached to the current session
                    dso = context.reloadEntity(dso);
                    doiRow.setDSpaceObject(dso);  // Update the DOI object with the reloaded DSpaceObject

                    operation.perform(context, provider, doiRow);
                    count++;

                    if (count % batchSize == 0) {
                        context.commit();
                        if (!quiet) {
                            System.out.println("Processed " + count + " DOIs for " + operationName + ".");
                        }
                    }

                    if (!quiet) {
                        System.out.println("Successfully " + operationName + " DOI " + DOI.SCHEME + doiRow.getDoi() + ".");
                    }
                } catch (IdentifierException | SQLException ex) {
                    LOG.error("Error " + operationName + " DOI " + doiRow.getDoi(), ex);
                }

                if (count % batchSize == 0) {
                    context.uncacheEntity(doiRow);
                }
            }

            if (count % batchSize != 0) {
                context.commit();
            }

        } catch (SQLException ex) {
            LOG.error("Database error during DOI " + operationName + " process", ex);
            try {
                context.rollback();
            } catch (SQLException e) {
                LOG.error("Error rolling back transaction", e);
            }
        }
    }

    public void update(List<DOI> doiList) {
        batchProcess(doiList, (context, provider, doi) -> 
            provider.updateMetadataOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi()),
            "update");
    }

    public void register(List<DOI> doiList) {
        batchProcess(doiList, (context, provider, doi) -> 
            provider.registerOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi(), this.filter),
            "register");
    }

    public void reserve(List<DOI> doiList) {
        batchProcess(doiList, (context, provider, doi) -> 
            provider.reserveOnline(context, doi.getDSpaceObject(), DOI.SCHEME + doi.getDoi(), this.filter),
            "reserve");
    }

    public void delete(List<DOI> doiList) {
        batchProcess(doiList, (context, provider, doi) -> 
            provider.deleteOnline(context, DOI.SCHEME + doi.getDoi()),
            "delete");
    }

}