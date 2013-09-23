/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.io.IOException;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
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
 */
public class DOIOrganiser {

    private static final Logger LOG = Logger.getLogger(DOIOrganiser.class);
    
    private DOIIdentifierProvider provider;
    private Context context;

    /**
    * DSpace Context object
    */
    //private  Context context;

    /**
     * Blanked off constructor, this class should be used as a command line
     * tool.
     *
     */
    private DOIOrganiser() {}
    
    public DOIOrganiser(Context context, DOIIdentifierProvider provider)
    {
        this.context = context;
        this.provider = provider;
    }

    public static void main(String[] args)
    {
        LOG.debug("Starting DOI organiser ");

        // Initialize command line interface
        Options options = initializeOptions(args);
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

        DOIOrganiser myself;
        myself = new DOIOrganiser(context, new DSpace().getSingletonService(DOIIdentifierProvider.class));


        // user asks for help
        if (line.hasOption('h'))
        {
            helpformater.printHelp("DOI organiser\n", options);
        }

        if (line.hasOption('l'))
        {
            myself.getDOIsByStatus("toBeReserved", "toBeRegistered");
        }

        if (line.hasOption('s'))
        {
            myself.doiRequest("toBeReserved");
        }

        if (line.hasOption('r'))
        {
            myself.doiRequest("toBeRegistered");
        }
        
        if(line.hasOption('g'))
        {
            String identifier = line.getOptionValue('g');
            
            if(null == identifier)
            {
                helpformater.printHelp("DOI organiser\n", options);
            }
            else
            {
                myself.doiRequestByDOI(identifier, "toBeRegistered");
                //doiRequestByItemID_Handle(context, itemID_Hdl,"toBeRegistered", doiIdentifierProvider);
            }
        }
        
        if(line.hasOption('v'))
        {
             String identifier = line.getOptionValue('v');
             
            if(null == identifier)
            {
                helpformater.printHelp("DOI organiser\n", options);
            }
            else
            {
                myself.doiRequestByDOI(identifier,"toBeReserved");
                //doiRequestByItemID_Handle(context, itemID_Hdl,"toBeReserved", doiIdentifierProvider);
            }
        }
        
        if(line.hasOption('u'))
        {
            String argument = line.getOptionValue('u');
            
            if(null == argument)
            {
                helpformater.printHelp("DOI organiser\n", options);
            }
            else
            {
                myself.update(argument);
            }
        }
        try 
        {
            context.complete();
        } 
        catch (SQLException sqle)
        {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

        LOG.debug("Ending Doi organiser");
    }

    public static Options initializeOptions(String[] args)
    {
        // create an options object 
        Options options = new Options();

        // add option to options

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "list", false,
                "List all objects to be reserved or registered ");
        options.addOption("r", "register-all", false,
                "register all to be registered identifiers online.");
        options.addOption("s", "reserve-all", false,
                "Reserve all to be reserved identifiers online.\n");
        
        Option registerDoi = OptionBuilder.withArgName("DOI identifier, ItemID or Handle")
                .hasArgs(1)
                .withDescription("Register a specified identifier. "
                + "You can specify the identifier by ItemID, Handle or DOI.")
                .create("register-doi");
        
        options.addOption(registerDoi);
        
        Option reserveDoi = OptionBuilder.withArgName("DOI identifier, ItemID or Handle")
                .hasArgs(1)
                .withDescription("Reserve a specified identifier online. "
                + "You can specify the identifier by ItemID, Handle or DOI.")
                .create("reserve-doi");

        options.addOption(reserveDoi);

        Option update = OptionBuilder.withArgName("DOI identifier, ItemID or Handle")
                .hasArgs(1)
                .withDescription("Update online an object for a given DOI identifier"
                + " or ItemID or Handle. A DOI identifier or an ItemID or a Handle is needed. ")
                .withLongOpt("update")
                .create('u');

        options.addOption(update);
        
        return options;
    }
    

    public TableRowIterator getDOIsByStatus(String ... status)
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

    public void doiRequest(String status)
            throws RuntimeException
    {
        try 
        {
            TableRowIterator iterator = this.getDOIsByStatus(status);
            
            while (iterator.hasNext())
            {
                TableRow doiRow = iterator.next();
                this.runRequest(doiRow, status);
            }
            iterator.close();
            
        } 
        catch (SQLException ex) 
        {
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        } 
    }
    
    public void runRequest(TableRow doiRow, String status)
    {
        try
        {
            DSpaceObject dso = DSpaceObject.find(context,
                    doiRow.getIntColumn("resource_type_id"),
                    doiRow.getIntColumn("resource_id"));

            if (status.equals("toBeRegistered"))
            {
                provider.registerOnline(context,
                        dso,
                        doiRow.getStringColumn("doi"));
            }
            else
            {
                provider.reserveOnline(context,
                        dso,
                        doiRow.getStringColumn("doi"));
            }
        }
        catch (SQLException ex) 
        { 
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        } 
        catch (IdentifierException ex) 
        {
            if (ex instanceof DOIIdentifierException) 
            {
                DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
                
                LOG.error("It wasn't possible to reserve or to register "
                         + "the identifier online. Exceptions code:  " 
                         + doiIdentifierException.codeToString(doiIdentifierException.getCode()),
                           ex);
            }
            
            else 
            LOG.error(ex);
        } 
        catch (IllegalArgumentException ex) 
        {
            LOG.error(ex);
        }
    }
    
    public void doiRequestByDOI(String identifier, String status)
    {
        try
        {
           String doi = DOI.formatIdentifier(identifier);
           TableRow doiRow = DatabaseManager.findByUnique(context, "Doi", "doi", doi.substring(DOI.SCHEME.length()));
           
           if(null == doiRow) LOG.error("Identifier: "+ identifier + " is not fund. ");
           this.runRequest(doiRow, status);
        } 
        catch (SQLException ex) 
        {
            LOG.error("It wasn't possible to connect to the Database",ex);
        } 
        catch (IdentifierException ex) 
        { 
            if (ex instanceof DOIIdentifierException) 
            {
                DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
                
                LOG.error("It wasn't possible to format the identifier. Exceptions code:  " 
                         + doiIdentifierException.codeToString(doiIdentifierException.getCode()), ex);
            }
            
            else 
            LOG.error("It wasn't possible to format the identifier.",ex);
        }
     }
    
    
    public void doiRequestByItemID_Handle(String itemID_Hdl, String status) 
    {
        TableRow row = null;
        try
        {
            if (itemID_Hdl.matches("\\d*"))
            {
                Integer itemID = Integer.valueOf(itemID_Hdl);
                row = DatabaseManager.findByUnique(context, "Item", "item_id", itemID);
                
                if (null == row) LOG.error("ItemID: " + itemID + " is not fund. ");
                
                this.runRequestByItemID_Handle(Constants.ITEM, itemID, status);
            } 
            else 
            {
                row = DatabaseManager.findByUnique(context, "Handle", "handle", itemID_Hdl);

                if (null == row) LOG.error("Handle: " + itemID_Hdl + " is not fund. ");
                
                this.runRequestByItemID_Handle(row.getIntColumn("resource_type_id"),
                        row.getIntColumn("resource_id"), status);
            }
        } 
        catch (SQLException ex) 
        {
            LOG.error("It wasn't possible to connect to the Database", ex);
        }
    }
    
    public void runRequestByItemID_Handle(Integer resource_type_id, Integer resource_id, String status)
    {
            DSpaceObject dso = null;
        try 
        {
            dso = DSpaceObject.find(context, resource_type_id, resource_id);
            
            if (status.equals("toBeRegistered")) 
            {
                provider.register(context, dso);
            } 
            else 
            {
                String identifier = provider.mint(context, dso);
                dso.update();
                provider.reserve(context, dso, identifier);
            }
        }  
        catch (SQLException ex) 
        {
            LOG.error("It wasn't possible to connect to the Database", ex);
        } 
        catch (IdentifierException ex) 
        {
            if (ex instanceof DOIIdentifierException) 
            {
                DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
                
                LOG.error("It wasn't possible to reserved or to registered an DOI"
                         + " Identifier for the object. Exceptions code: " 
                         + doiIdentifierException.codeToString(doiIdentifierException.getCode()), ex);
            }
            
            else 
                LOG.error("It wasn't possible to reserved or to registered an"
                         + " DOI Identifier for the objact", ex);
        } 
        catch (AuthorizeException ex) 
        {
            LOG.error("It wasn't possible to update the dspace object in the database", ex);
        }
    }
    public void update(String argument)
    {
       
        TableRow row = null;
        TableRow doiRow = null;
        String sql = "Select * From Doi Where resource_type_id = ? and resource_id = ? ";
        try 
        {
            if (argument.matches("\\d*")) 
            {
                Integer itemID = Integer.valueOf(argument);
                row = DatabaseManager.findByUnique(context, "Item", "item_id", itemID);
                
                if (null != row) 
                {
                    doiRow = DatabaseManager.querySingleTable(context,
                            "Doi", sql, row.getIntColumn("submitter_id"), row.getIntColumn("item_id"));
                    
                    if (null != doiRow) 
                    {
                        DSpaceObject dso = DSpaceObject.find(context,
                                doiRow.getIntColumn("resource_type_id"),
                                doiRow.getIntColumn("resource_id"));
                        provider.updateMetadata(context, dso, doiRow.getStringColumn("doi"));
                        return;
                    } 
                    else 
                    {
                        LOG.error("There are no DOI Identifier for this Value : " + argument);
                    }
                }
                else
                {
                    LOG.error("This Item : " + argument + "doesn't exist");
                }
            }
            row = DatabaseManager.findByUnique(context, "Handle", "handle", argument);
            
            if (null != row) 
            {
                doiRow = DatabaseManager.querySingleTable(context,
                        "Doi", sql, row.getIntColumn("resource_type_id"), row.getIntColumn("resource_id"));
                
                if (null != doiRow) 
                {
                    DSpaceObject dso = DSpaceObject.find(context,
                            doiRow.getIntColumn("resource_type_id"),
                            doiRow.getIntColumn("resource_id"));
                    provider.updateMetadata(context, dso, doiRow.getStringColumn("doi"));
                    return;
                } 
                else 
                {
                    LOG.error("There are no DOI Identifier for this Value : " + argument);
                }
            } 
            else 
            {
                LOG.error("This Handle: " + argument + "doesn't exist");
            }
            
            String doi = DOI.formatIdentifier(argument);
            row = DatabaseManager.findByUnique(context, "Doi", "doi", doi.substring(DOI.SCHEME.length()));
            
            if (null != row) 
            {
                DSpaceObject dso = DSpaceObject.find(context,
                        row.getIntColumn("resource_type_id"),
                        row.getIntColumn("resource_id"));
                provider.updateMetadata(context, dso, row.getStringColumn("doi"));
            } 
            else 
            {
                LOG.error("This DOI identifier: " + argument + "doesn't exist");
            }
        }
        catch (SQLException ex) 
        {
            LOG.error(ex);
        } 
        catch (IdentifierException ex) 
        {
            if (ex instanceof DOIIdentifierException) 
            {
                DOIIdentifierException doiIdentifierException = (DOIIdentifierException) ex;
                
                LOG.error("It wasn't possible to update The object.  Exceptions code: " 
                         + doiIdentifierException.codeToString(doiIdentifierException.getCode()), ex);
            }
            else 
                LOG.error("It wasn't possible to update The object", ex);
        }
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

            }
        }
        catch (Exception e) {
            LOG.warn("Unable to send email alert", e);
        }
    }
    
}
