/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import java.sql.SQLException;
import java.util.logging.Level;
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
import org.dspace.core.Constants;
import org.dspace.core.Context;
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

    /**
    * DSpace Context object
    */
    //private  Context context;

    /**
     * Blanked off constructor, this class should be used as a command line
     * tool.
     *
     */
    private DOIOrganiser()
           // throws SQLException
    {
      //  context = new Context();
    }

    public static void main(String[] args)
    {
        LOG.info("Starting DOI organiser ");

        // set up command line and Command line parser 
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object 
        Options options = new Options();

        // add option to options

        options.addOption("h", "help", false, "Help");
        options.addOption("l", "list", false,
                "List all objects to be reserved or registered ");
        options.addOption("r", "register", false,
                "register all to be registered identifiers online.");
        options.addOption("s", "reserve", false,
                "Reserve all to be reserved identifiers online.\n");
        
        Option registerDoi =  OptionBuilder.withArgName("DOI identifier")
                     .hasArgs(1)
                     .withDescription("Register an identifier online. "
                     + "An DOI identifier is needed.").create('g');
        
        options.addOption(registerDoi);
        
        Option reserveDoi = OptionBuilder.withArgName("DOI identifier")
                     .hasArgs(1)
                     .withDescription("Reserve an identifier online."
                     + " An DOI identifier is needed. ").create('v');
        
        options.addOption(reserveDoi);
        
        Option registerByItemOrHandle = OptionBuilder.withArgName("ItemID or Handle")
                     .hasArgs(1)
                     .withDescription("Reserve an identifier for a given ItemID or Handle."
                     + " An ItemID or a Handle is needed. ").create('t');
        
        options.addOption(registerByItemOrHandle);
        
        Option reserveByItemOrHandle = OptionBuilder.withArgName("ItemID or Handle")
                     .hasArgs(1)
                     .withDescription("Reserve an identifier  for a given ItemID or Handle."
                     + " An ItemID or a Handle is needed. ").create('i');
        
        options.addOption(reserveByItemOrHandle);
        
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
        
        try 
        {
            line = parser.parse(options, args);
        } 
        catch (ParseException ex)
        {
            LOG.fatal(ex);
            System.exit(1);
        }

        // user asks for help
        if (line.hasOption('h'))
        {
            printHelp(options);
        }

        if (line.hasOption('l'))
        {
            listDoiTable(context);
        }

        if (line.hasOption('s'))
        {
            doiRequest(context, "toBeReserved");
        }

        if (line.hasOption('r'))
        {
            doiRequest(context, "toBeRegistered");
        }
        
        if(line.hasOption('g')){
            String identifier = line.getOptionValue('g');
            if(null == identifier){
                LOG.info("A DOI identifier is needed");
                throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
            }else{
                doiRequestByDOI(context, identifier,"toBeRegistered");
            }
        }
        if(line.hasOption('v')){
             String identifier = line.getOptionValue('v');
            if(null == identifier){
                LOG.info("A DOI identifier is needed");
                throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
            }
            else
            {
                doiRequestByDOI(context, identifier,"toBeReserved");
            }
        }
        if(line.hasOption('t')){
            String itemID_Hdl = line.getOptionValue('t');
            if(null == itemID_Hdl){
                LOG.info("A ItemID or a Handle is needed");
                throw new IllegalArgumentException("ItemID or Handle is null.", new NullPointerException());
            }
            else
            {
                doiRequestByItemID_Handle(context, itemID_Hdl,"toBeRegistered");
            }
        }
        if(line.hasOption('i')){
            String itemID_Hdl = line.getOptionValue('i');
            if(null == itemID_Hdl){
                LOG.info("A ItemID or a Handle is needed");
                throw new IllegalArgumentException("ItemID or Handle is null.", new NullPointerException());
            }
            else
            {
                doiRequestByItemID_Handle(context, itemID_Hdl,"toBeReserved");
            }
        }
        try {
            context.complete();
        } catch (SQLException sqle)
        {
            System.err.println("Cannot save changes to database: " + sqle.getMessage());
            System.exit(-1);
        }

        LOG.info("Ending Doi organiser");
    }

    /**
     * Print the help options for the user
     *
     * @param options that are available for the user
     */
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();

        myhelp.printHelp("DOI organiser\n", options);
    }

    private static TableRowIterator listDoiTableByStatus(Context context, String status)
    {
        TableRowIterator doiIterator = null;
        try 
        {   
            String sql = "Select * From Doi Where status = ?";

            doiIterator = DatabaseManager.queryTable(context, "Doi", sql, status);
        }
        catch (SQLException ex)
        {
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        }
        return doiIterator;
    }

    private static void listDoiTable(Context context)
    {
        try 
        {
            String sql = "Select * From Doi Where status = ? or status = ? ";

            TableRowIterator doiIterator = DatabaseManager.queryTable(context, 
                                           "Doi", sql, "toBeReserved", "toBeRegistered");
            while (doiIterator.hasNext())
            {
                TableRow doiRow = doiIterator.next();
                System.out.println(doiRow.toString());
            }
            doiIterator.close();
        } 
        catch (SQLException ex)
        {
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        }
    }
    
    private static void doiRequest(Context context, String status) throws RuntimeException
    {
        try 
        {
            TableRowIterator iterator = listDoiTableByStatus(context, status);
            
            while (iterator.hasNext())
            {
                TableRow doiRow = iterator.next();
                runRequest(context, doiRow, status);
            }
            iterator.close();
        } 
        catch (SQLException ex) 
        {
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        } 
    }
    
    private static void  runRequest(Context context, TableRow doiRow, String status){
            DOIIdentifierProvider doiIdentifierProvider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        try {
            DSpaceObject dso = DSpaceObject.find(context,
                                                         doiRow.getIntColumn("resource_type_id"),
                                                         doiRow.getIntColumn("resource_id"));

                    if (status.equals("toBeRegistered"))
                    {
                        doiIdentifierProvider.registeredDOIOnline(context,
                                                                        dso,
                                                                        doiRow.getStringColumn("doi"));
                    }
                    else
                    {
                        doiIdentifierProvider.reserveDOIOnline(context,
                                                                     dso, 
                                                                     doiRow.getStringColumn("doi"));
                    }
        } catch (SQLException ex) { 
            LOG.error("Error while trying to get data from database", ex);
            throw new RuntimeException("Error while trying to get data from database", ex);
        } catch (IdentifierException ex) {
            LOG.error(ex);
        } catch (IllegalArgumentException ex) {
            LOG.error(ex);
        }
    }
    
    private static void doiRequestByDOI(Context context, String identifier, String status){
        try {
           DOIIdentifierProvider doiIdentifierProvider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
           String doi = doiIdentifierProvider.formatIdentifier(identifier);
           TableRow doiRow = DatabaseManager.findByUnique(context, "Doi", "doi", doi.substring(DOI.SCHEME.length()));
           if(null == doiRow) LOG.error("Identifier: "+ identifier + " is not fund. ");
           runRequest(context, doiRow, status);
        } catch (SQLException ex) {
            LOG.error("It wasn't possible to connect to the Database",ex);
        } catch (IdentifierException ex) {
            LOG.error(ex);
        }
     }
    
    
    private static void doiRequestByItemID_Handle(Context context, String itemID_Hdl, String status) {
        TableRow row = null;
        try
        {
            if (itemID_Hdl.matches("\\d*"))
            {
                Integer itemID = Integer.valueOf(itemID_Hdl);
                row = DatabaseManager.findByUnique(context, "Item", "item_id", itemID);
                
                if (null == row) LOG.error("ItemID: " + itemID + " is not fund. ");
                
                runRequestByItemID_Handle(context, Constants.ITEM, itemID, status);
            } 
            else 
            {
                row = DatabaseManager.findByUnique(context, "Handle", "handle", itemID_Hdl);

                if (null == row) LOG.error("Handle: " + itemID_Hdl + " is not fund. ");
                
                runRequestByItemID_Handle(context, row.getIntColumn("resource_type_id"), row.getIntColumn("resource_id"), status);
            }
        } 
        catch (SQLException ex) 
        {
            LOG.error("It wasn't possible to connect to the Database", ex);
        }
    }
    
    private static void runRequestByItemID_Handle(Context context,Integer resource_type_id,Integer resource_id, String status ){
            DSpaceObject dso = null;
            DOIIdentifierProvider doiIdentifierProvider = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        try {
            dso = DSpaceObject.find(context, resource_type_id, resource_id);
            if (status.equals("toBeRegistered")) {
                doiIdentifierProvider.register(context, dso);
            } else {
                String identifier = doiIdentifierProvider.mint(context, dso);
                dso.update();
                doiIdentifierProvider.reserve(context, dso, identifier);
            }
        }  catch (SQLException ex) {
            LOG.error("It wasn't possible to connect to the Database", ex);
        } catch (IdentifierException ex) {
            LOG.error("It wasn't possible to reserved or to registered an Identifier for the objact", ex);
        } catch (AuthorizeException ex) {
            LOG.error("It wasn't possible to update the dspace objact", ex);
        }
    }
}
