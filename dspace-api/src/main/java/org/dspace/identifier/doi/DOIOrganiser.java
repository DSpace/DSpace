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
import java.util.Date;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;


/**
 *
 * @author Marsa Haoua
 * @author Pascal-Nicolas Becker
 */
public class DOIOrganiser {

    private static final Logger LOG = Logger.getLogger(DOIOrganiser.class);

    private DOIIdentifierProvider provider;
    private Context context;
    private boolean quiet;

    public DOIOrganiser(Context context, DOIIdentifierProvider provider)
    {
        this.context = context;
        this.provider = provider;
        this.quiet = false;
    }

    public static void main(String[] args)
    {
        LOG.debug("Starting DOI organiser ");

        // setup Context
        Context context = null;
        try {
            context = new Context();
        }
        catch (SQLException sqle)
        {
            System.err.println("Can't connect to database: " + sqle.getMessage());
            System.exit(-1);
        }
        // Started from commandline, don't use the authentication system.
        context.turnOffAuthorisationSystem();

        DOIOrganiser organiser = new DOIOrganiser(context, new DSpace().getSingletonService(DOIIdentifierProvider.class));
        
        // run command line interface
        runCLI(context, organiser, args);
        
        try 
        {
            context.complete();
        } 
        catch (SQLException sqle)
        {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

    }

    public static void runCLI(Context context, DOIOrganiser organiser, String[] args)
    {
        // initlize options
        Options options = new Options();

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "list", false,
                "List all objects to be reserved, registered, deleted of updated ");
        options.addOption("r", "register-all", false,
                "Perform online registration for all identifiers queued for registration.");
        options.addOption("s", "reserve-all", false,
                "Perform online reservation for all identifiers queued for reservation.");
        options.addOption("u", "update-all", false,
                "Perform online metadata update for all identifiers queued for metadata update.");
        options.addOption("d", "delete-all", false,
                "Perform online deletion for all identifiers queued for deletion.");
        
        options.addOption("q", "quiet", false,
                "Turn the command line output off.");
        
        Option registerDoi = OptionBuilder.withArgName("DOI|ItemID|handle")
                .withLongOpt("register-doi")
                .hasArgs(1)
                .withDescription("Register a specified identifier. "
                + "You can specify the identifier by ItemID, Handle or DOI.")
                .create();
        
        options.addOption(registerDoi);
        
        Option reserveDoi = OptionBuilder.withArgName("DOI|ItemID|handle")
                .withLongOpt("reserve-doi")
                .hasArgs(1)
                .withDescription("Reserve a specified identifier online. "
                + "You can specify the identifier by ItemID, Handle or DOI.")
                .create();

        options.addOption(reserveDoi);

        Option update = OptionBuilder.withArgName("DOI|ItemID|handle")
                .hasArgs(1)
                .withDescription("Update online an object for a given DOI identifier"
                + " or ItemID or Handle. A DOI identifier or an ItemID or a Handle is needed.\n")
                .withLongOpt("update-doi")
                .create();

        options.addOption(update);
        
        Option delete = OptionBuilder.withArgName("DOI identifier")
                .withLongOpt("delete-doi")
                .hasArgs(1)
                .withDescription("Delete a specified identifier.")
                .create();

        options.addOption(delete);
        

        // initialize parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        HelpFormatter helpformater = new HelpFormatter();

        try 
        {
            line = parser.parse(options, args);
        } 
        catch (ParseException ex)
        {
            LOG.fatal(ex);
            System.exit(1);
        }


        // process options
        // user asks for help
        if (line.hasOption('h') || 0 == line.getOptions().length)
        {
            helpformater.printHelp("\nDOI organiser\n", options);
        }
        
        if (line.hasOption('q')) 
        {
            organiser.setQuiet();
        }
        
        if (line.hasOption('l'))
        {
            organiser.list("reservation", null, null, DOIIdentifierProvider.TO_BE_RESERVERED);
            organiser.list("registration", null, null, DOIIdentifierProvider.TO_BE_REGISTERED);
            organiser.list("update", null, null,
                    DOIIdentifierProvider.UPDATE_BEFORE_REGISTERATION,
                    DOIIdentifierProvider.UPDATE_REGISTERED,
                    DOIIdentifierProvider.UPDATE_RESERVERED);
            organiser.list("deletion", null, null, DOIIdentifierProvider.TO_BE_DELETED);
        }

        if (line.hasOption('s'))
        {
            TableRowIterator it = organiser
                                    .getDOIsByStatus(DOIIdentifierProvider.TO_BE_RESERVERED);
           
            try { 
                if (!it.hasNext()) 
                {
                    System.err.println("There are no objects in the database "
                            + "that could be reserved.");
                }
                
                while (it.hasNext())
                {
                    TableRow doiRow = it.next();
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"), 
                            doiRow.getIntColumn("resource_id"));
                    organiser.reserve(doiRow, dso);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption('r'))
        {
            TableRowIterator it = organiser
                                    .getDOIsByStatus(DOIIdentifierProvider.TO_BE_REGISTERED);
            
            try {
                if (!it.hasNext()) 
                {
                    System.err.println("There are no objects in the database "
                            + "that could be registered.");
                }
                while (it.hasNext())
                {
                    TableRow doiRow = it.next();
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"), 
                            doiRow.getIntColumn("resource_id"));
                    organiser.register(doiRow, dso);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        
        if (line.hasOption('u'))
        {
            TableRowIterator it = organiser.getDOIsByStatus(
                    DOIIdentifierProvider.UPDATE_BEFORE_REGISTERATION,
                    DOIIdentifierProvider.UPDATE_RESERVERED,
                    DOIIdentifierProvider.UPDATE_REGISTERED);
           
            try { 
                if (!it.hasNext()) 
                {
                    System.err.println("There are no objects in the database "
                            + "whose metadata needs an update.");
                }
                
                while (it.hasNext())
                {
                    TableRow doiRow = it.next();
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"), 
                            doiRow.getIntColumn("resource_id"));
                    organiser.update(doiRow, dso);
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        if (line.hasOption('d'))
        {
            TableRowIterator it = organiser
                                    .getDOIsByStatus(DOIIdentifierProvider.TO_BE_DELETED);
           
            try { 
                if (!it.hasNext()) 
                {
                    System.err.println("There are no objects in the database "
                            + "that could be deleted.");
                }
                
                while (it.hasNext())
                {
                    TableRow doiRow = it.next();
                    organiser.delete(doiRow.getStringColumn("doi"));
                }
            } catch (SQLException ex) {
                System.err.println("Error in database connection:" + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        
        if(line.hasOption("reserve-doi"))
        {
            String identifier = line.getOptionValue("reserve-doi");
            
            if(null == identifier)
            {
                helpformater.printHelp("\nDOI organiser\n", options);
            }
            else
            {
                try {
                    TableRow doiRow = organiser.findTableRow(identifier);
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"), 
                            doiRow.getIntColumn("resource_id"));
                    organiser.reserve(doiRow, dso);
                } 
                catch (SQLException ex) 
                {
                    LOG.error(ex);
                } 
                catch (IllegalArgumentException ex) 
                {
                    LOG.error(ex);
                } 
                catch (IllegalStateException ex) 
                {
                    LOG.error(ex);
                } catch (IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }
        
        if(line.hasOption("register-doi"))
        {
            String identifier = line.getOptionValue("register-doi");
            
            if(null == identifier)
            {
                helpformater.printHelp("\nDOI organiser\n", options);
            }
            else
            {
                try {
                    TableRow doiRow = organiser.findTableRow(identifier);
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"),
                            doiRow.getIntColumn("resource_id"));
                    organiser.register(doiRow, dso);
                } catch (SQLException ex) {
                    LOG.error(ex);
                } catch (IllegalArgumentException ex) {
                    LOG.error(ex);
                } catch (IllegalStateException ex) {
                    LOG.error(ex);
                } catch (IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }
        
        if(line.hasOption("update-doi"))
        {
            String identifier = line.getOptionValue('u');
            
            if(null == identifier)
            {
                helpformater.printHelp("\nDOI organiser\n", options);
            }
            else
            {
                try {
                    TableRow doiRow = organiser.findTableRow(identifier);
                    DSpaceObject dso = DSpaceObject.find(
                            context, 
                            doiRow.getIntColumn("resource_type_id"), 
                            doiRow.getIntColumn("resource_id"));
                    organiser.update(doiRow, dso);
                } catch (SQLException ex) {
                    LOG.error(ex);
                } catch (IllegalArgumentException ex) {
                    LOG.error(ex);
                } catch (IllegalStateException ex) {
                    LOG.error(ex);
                } catch (IdentifierException ex) {
                    LOG.error(ex);
                }
            }
        }
        
        if(line.hasOption("delete-doi"))
        {
            String identifier = line.getOptionValue('d');

            if (null == identifier) 
            {
                helpformater.printHelp("\nDOI organiser\n", options);
            } 
            else {
                try {
                    organiser.delete(identifier);
                } catch (SQLException ex) {
                    LOG.error(ex);
                } catch (IllegalArgumentException ex) {
                    LOG.error(ex);
                }
            }
        }

    }
    

    public TableRowIterator getDOIsByStatus(Integer ... status)
    {
        try 
        {   
            String sql = "SELECT * FROM Doi";
            for (int i = 0; i < status.length ; i++)
            {
                if (0 == i)
                {
                    sql += " WHERE ";
                }
                else
                {
                    sql += " OR ";
                }
                sql += " status = ?";
            }

            if (status.length < 1)
            {
                return DatabaseManager.queryTable(context, "Doi", sql);
            }
            return DatabaseManager.queryTable(context, "Doi", sql, status);
        }
        catch (SQLException ex)
        {
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        }
    }
    
    public void list(String processName, PrintStream out, PrintStream err, Integer ... status)
    {
        String indent = "    ";
        if (null == out)
        {
            out = System.out;
        }
        if (null == err)
        {
            err = System.err;
        }
        
        TableRowIterator it = this.getDOIsByStatus(status);

        try
        {
            if (it.hasNext())
            {
                out.println("DOIs queued for " + processName + ": ");
            }
            else
            {
                out.println("There are no DOIs queued for " + processName + ".");
            }
            while(it.hasNext())
            {
                TableRow doiRow = it.next();
                DSpaceObject dso = DSpaceObject.find(context,
                        doiRow.getIntColumn("resource_type_id"),
                        doiRow.getIntColumn("resource_id"));
                out.print(indent + DOI.SCHEME + doiRow.getStringColumn("doi"));
                if (null != dso)
                {
                    out.println(" (belongs to item with handle " + dso.getHandle() + ")");
                }
                else
                {
                    out.println(" (cannot determine handle of assigned object)");
                }
            }
            out.println("");
        }
        catch (SQLException ex)
        {
            err.println("Error in database Connection: " + ex.getMessage());
            ex.printStackTrace(err);
        }
        finally
        {
            it.close();
        }
    }

    public void register(TableRow doiRow, DSpaceObject dso) throws SQLException
    {
        if (Constants.ITEM != dso.getType())
        {
            throw new IllegalArgumentException("Currenty DSpace supports DOIs for Items only.");
        }
        
        try {
            provider.registerOnline(context, dso,
                    DOI.SCHEME + doiRow.getStringColumn("doi"));
            
            if(!quiet)
            {
                System.out.println("This identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi") 
                                 + " is successfully registered.");
            }
        } 
        catch (IdentifierException ex) 
        {
            if (!(ex instanceof DOIIdentifierException))
            {
                LOG.error("It wasn't possible to register this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi")
                                 + " online. ", ex);
            }

            DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
           
            try 
            {
                sendAlertMail("Register", dso,
                              DOI.SCHEME + doiRow.getStringColumn("doi"),
                              doiIdentifierException.codeToString(doiIdentifierException
                                                                    .getCode())); 
            }
            catch (IOException ioe) 
            {
                LOG.error("Couldn't send mail", ioe);
            }

            LOG.error("It wasn't possible to register this identifier : " 
                    + DOI.SCHEME + doiRow.getStringColumn("doi")
                    + " online. Exceptions code: "
                    + doiIdentifierException
                        .codeToString(doiIdentifierException.getCode()), ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to register this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
             
        }
        catch (IllegalArgumentException ex) 
        {
            LOG.error("Database table DOI contains a DOI that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to register this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
            
            throw new IllegalStateException("Database table DOI contains a DOI "
                    + " that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
        } 
        catch (SQLException ex) 
        {
            LOG.error("Error while trying to get data from database", ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to register this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
            throw new RuntimeException("Error while trying to get data from database", ex);
        }
    }
    
    public void reserve(TableRow doiRow, DSpaceObject dso) throws SQLException
    {
        if (Constants.ITEM != dso.getType())
        {
            throw new IllegalArgumentException("Currenty DSpace supports DOIs for Items only.");
        }
        
        try 
        {
            provider.reserveOnline(context, dso, 
                    DOI.SCHEME + doiRow.getStringColumn("doi"));
            
            if(!quiet)
            {
                System.out.println("This identifier : " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi") 
                                 + " is successfully reserved.");
            }
        } 
        catch (IdentifierException ex) 
        {
            if (!(ex instanceof DOIIdentifierException)) 
            {
                LOG.error("It wasn't possible to register this identifier : " 
                    + DOI.SCHEME + doiRow.getStringColumn("doi")
                    + " online. ",ex);
            }
            
            DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
            
            try 
            {
                sendAlertMail("Reserve", dso,
                              DOI.SCHEME + doiRow.getStringColumn("doi"),
                              DOIIdentifierException.codeToString(
                                    doiIdentifierException.getCode()));
            } 
            catch (IOException ioe) 
            {
                LOG.error("Couldn't send mail", ioe);
            }

            LOG.error("It wasn't possible to reserve the identifier online. "
                    + " Exceptions code:  " 
                    + DOIIdentifierException
                          .codeToString(doiIdentifierException.getCode()), ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to reserve this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
        } 
        catch (IllegalArgumentException ex) 
        {
            LOG.error("Database table DOI contains a DOI that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to reserve this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
            throw new IllegalStateException("Database table DOI contains a DOI "
                    + " that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
        } 
        catch (SQLException ex) 
        {
            LOG.error("Error while trying to get data from database", ex);
             
            if(!quiet)
            {
                System.err.println("It wasn't possible to reserve this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
            throw new RuntimeException("Error while trying to get data from database", ex);

        }
    }
    
    public void update(TableRow doiRow, DSpaceObject dso)
    {
        if (Constants.ITEM != dso.getType())
        {
            throw new IllegalArgumentException("Currenty DSpace supports DOIs "
                    + "for Items only.");
        }
        
        try 
        {
            provider.updateMetadataOnline(context, dso,
                    DOI.SCHEME + doiRow.getStringColumn("doi"));
            
            if(!quiet)
            {
                System.out.println("Successfully updated metadata of DOI " + DOI.SCHEME
                        + doiRow.getStringColumn("doi") + ".");
            }
        }
        catch (IdentifierException ex) 
        {
            if (!(ex instanceof DOIIdentifierException)) 
            {
                LOG.error("It wasn't possible to register the identifier online. ",ex);
            }
            
            DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
            
            try 
            {
                sendAlertMail("Update", dso,
                              DOI.SCHEME + doiRow.getStringColumn("doi"),
                              doiIdentifierException.codeToString(doiIdentifierException
                                                                    .getCode()));
            } 
            catch (IOException ioe) 
            {
                LOG.error("Couldn't send mail", ioe);
            }

            LOG.error("It wasn't possible to update this identifier:  "
                    + DOI.SCHEME + doiRow.getStringColumn("doi")
                    + " Exceptions code:  " 
                    + doiIdentifierException
                        .codeToString(doiIdentifierException.getCode()), ex);
           
            if(!quiet)
            {
                System.err.println("It wasn't possible to update this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
            
        } 
        catch (IllegalArgumentException ex) 
        {
            LOG.error("Database table DOI contains a DOI that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
            
             if(!quiet)
            {
                System.err.println("It wasn't possible to update this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
             
            throw new IllegalStateException("Database table DOI contains a DOI "
                    + " that is not valid: "
                    + DOI.SCHEME + doiRow.getStringColumn("doi") + "!", ex);
        } 
        catch (SQLException ex) 
        {
            LOG.error("It wasn't possible to connect to the Database!", ex);
        }
    }
    
    public void delete(String identifier) 
            throws SQLException
    {
        String doi = null;
        TableRow doiRow = null;
        
        try 
        {
            doi = DOI.formatIdentifier(identifier);
            
            // If there's no exception: we found a valid DOI. :)
            doiRow = DatabaseManager.findByUnique(context, "Doi", "doi",
                    doi.substring(DOI.SCHEME.length()));
           
            if (null == doiRow) 
            {
                throw new IllegalStateException("You specified a valid DOI,"
                        + " that is not stored in our database.");
            }
            provider.deleteOnline(context, doi);
             
            if (!quiet) 
            {
                System.err.println("It was possible to delete this identifier: "
                        + DOI.SCHEME + doiRow.getStringColumn("doi")
                        + " online.");
            }
        } 
        catch (DOIIdentifierException ex) 
        {
            // Identifier was not recognized as DOI.
            LOG.error("It wasn't possible to detect this identifier:  "
                    + identifier
                    + " Exceptions code:  "
                    + ex.codeToString(ex.getCode()), ex);

            if (!quiet) 
            {
                System.err.println("It wasn't possible to detect this identifier: "
                        + identifier);
            }
        } 
        catch (IllegalArgumentException ex) 
        {
            if (!quiet) 
            {
                System.err.println("It wasn't possible to delete this identifier: "
                        + DOI.SCHEME + doiRow.getStringColumn("doi")
                        + " online. Take a look in log file.");
            }
        }
    }
    
    /**
     * Finds the TableRow in the Doi table that belongs to the specified
     * DspaceObject.
     *
     * @param identifier Either an ItemID, a DOI or a handle. If the identifier
     * contains digits only we treat it as ItemID, if not we try to find a
     * matching doi or a handle (in this order).
     * @return The TableRow or null if the Object does not have a DOI.
     * @throws SQLException
     * @throws IllegalArgumentException If the identifier is null, an empty
     * String or specifies an DSpaceObject that is not an item. We currently
     * support DOIs for items only, but this may change once...
     * @throws IllegalStateException If the identifier was a valid DOI that is
     * not stored in our database or if it is a handle that is not bound to an
     * DSpaceObject.
     */
    public TableRow findTableRow(String identifier)
            throws SQLException, IllegalArgumentException, IllegalStateException, IdentifierException
    {
        if (null == identifier || identifier.isEmpty()) 
        {
            throw new IllegalArgumentException("Identifier is null or empty.");
        }

        String sql = "SELECT * FROM Doi WHERE resource_type_id = ? AND resource_id = ? ";
        TableRow doiRow = null;
        String doi = null;
        
        // detect it identifer is ItemID, handle or DOI.
        // try to detect ItemID
        if (identifier.matches("\\d*")) 
        {
            Integer itemID = Integer.valueOf(identifier);
            DSpaceObject dso = Item.find(context, itemID);

            if (null != dso) 
            {
                doiRow = DatabaseManager.querySingleTable(context, "Doi",
                        sql, Constants.ITEM, dso.getID());
                
                //Check if this Item has an Identifier, mint one if it doesn't
                if (null == doiRow) 
                {
                    doi = provider.mint(context, dso);
                    doiRow = DatabaseManager.findByUnique(context, "Doi", "doi",
                    doi.substring(DOI.SCHEME.length()));
                    return doiRow;
                }
                return doiRow;
            } 
            else 
            {
                throw new IllegalStateException("You specified an ItemID, "
                        + "that is not stored in our database.");
            }
        }

        // detect handle
        DSpaceObject dso = HandleManager.resolveToObject(context, identifier);

        if (null != dso) 
        {
            if (dso.getType() != Constants.ITEM) 
            {
                throw new IllegalArgumentException(
                        "Currently DSpace supports DOIs for Items only. "
                        + "Cannot process specified handle as it does not identify an Item.");
            }
            
            doiRow = DatabaseManager.querySingleTable(context, "Doi", sql,
                    Constants.ITEM, dso.getID());

            if (null == doiRow) 
            {
                doi = provider.mint(context, dso);
                doiRow = DatabaseManager.findByUnique(context, "Doi", "doi",
                        doi.substring(DOI.SCHEME.length()));
            }
            return doiRow;
        }
        // detect DOI
        try {
            doi = DOI.formatIdentifier(identifier);
            // If there's no exception: we found a valid DOI. :)
            doiRow = DatabaseManager.findByUnique(context, "Doi", "doi",
                    doi.substring(DOI.SCHEME.length()));
            if (null == doiRow)
            {
                throw new IllegalStateException("You specified a valid DOI,"
                        + " that is not stored in our database.");
            }
        }
        catch (DOIIdentifierException ex) 
        {
            // Identifier was not recognized as DOI.
            LOG.error("It wasn't possible to detect this identifier:  " 
                    + identifier
                    + " Exceptions code:  " 
                    + ex.codeToString(ex.getCode()), ex);
            
            if(!quiet)
            {
                System.err.println("It wasn't possible to detect this identifier: " 
                                 + DOI.SCHEME + doiRow.getStringColumn("doi"));
            }
        }

        return doiRow;
    }

    private void sendAlertMail(String action, DSpaceObject dso, String doi, String reason) 
            throws IOException
    {
        String recipient = ConfigurationManager.getProperty("alert.recipient");

        try
        {
            if (recipient != null)
            {
                Email email = Email.getEmail(
                        I18nUtil.getEmailFilename(Locale.getDefault(), "doi_maintenance_error"));
                email.addRecipient(recipient);
                email.addArgument(action);
                email.addArgument(new Date());
                email.addArgument(dso.getTypeText());
                email.addArgument(new Integer(dso.getID()));
                email.addArgument(doi);
                email.addArgument(reason);
                email.send();
                
                if (!quiet) 
                {
                    System.err.println("Email alert is sent.");
                }
            }
        }
        catch (Exception e) {
            LOG.warn("Unable to send email alert", e);
            if (!quiet) 
            {
                System.err.println("Unable to send email alert.");
            }
        }
    }

    private void setQuiet() 
    {
        this.quiet = true;
    }
    
}
