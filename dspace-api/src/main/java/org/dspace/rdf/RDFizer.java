/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.rdf.factory.RDFFactory;
import org.dspace.rdf.storage.RDFStorage;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class manages the handling of RDF data in DSpace. It generates
 * identifiers, it loads data, it manages the conversion of DSpace Objects into
 * RDF data. It can be used as instantiated object as well as CLI.
 * 
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFizer {
    
    private static final Logger log = Logger.getLogger(RDFizer.class);
    
    protected boolean stdout;
    protected boolean verbose;
    protected boolean dryrun;
    protected String lang;
    protected Context context;
    
    protected final ConfigurationService configurationService;
    protected final ContentServiceFactory contentServiceFactory;
    protected final CommunityService communityService;
    protected final ItemService itemService;
    protected final HandleService handleService;
    protected final RDFStorage storage;


    /**
     * Set to remember with DSpaceObject were converted or deleted from the 
     * triplestore already. This set is helpful when converting or deleting 
     * multiple DSpaceObjects (e.g. Communities with all Subcommunities and
     * Items).
     */
    protected Set<UUID> processed;

    public RDFizer()
    {
        this.stdout = false;
        this.verbose = false;
        this.dryrun = false;
        this.lang = "TURTLE";
        this.processed = new CopyOnWriteArraySet<UUID>();
        this.context = new Context(Context.Mode.READ_ONLY);
        
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.contentServiceFactory = ContentServiceFactory.getInstance();
        this.communityService = contentServiceFactory.getCommunityService();
        this.itemService = contentServiceFactory.getItemService();
        this.handleService = HandleServiceFactory.getInstance().getHandleService();
        this.storage = RDFFactory.getInstance().getRDFStorage();
    }
    
    /**
     * This method allows you to override the context used for conversion and to
     * determine which DSpaceObjects should be deleted from the triplestore,
     * consider well if this is really necessary.
     * If this method is not used the context of an anonymous user will be used. 
     * <p>
     * Please consider: If your triplestore offers a public sparql endpoint 
     * all information readable with the provided context will be exposed to 
     * public!
     * If you store your data in a private triplestore that does not provides 
     * public access, you might consider to use this method to convert all data 
     * stored in your repository.
     * </p>
     * 
     * @param context 
     */
    protected void overrideContext(Context context)
    {
        this.context = context;
    }

    /**
     * Returns whether all converted data is printed to stdout. Turtle will be
     * used as serialization.
     * @return {@code true} if print all generated data is to be printed to stdout
     */
    public boolean isStdout() {
        return stdout;
    }

    /**
     * Set this to true to print all generated data to stdout. The data will be
     * stored as well, unless {@code dryrun} is set true. Turtle will be used
     * as serialization.
     * @param stdout 
     */
    public void setStdout(boolean stdout) {
        this.stdout = stdout;
    }
    
    /**
     * Returns whether verbose information is printed to System.err. Probably 
     * this is helpful for CLI only.
     * @return {@code true} if verbose mode is on
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Set this to true to print verbose information to System.err. Probably 
     * this is helpful for CLI only.
     * @param verbose 
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Returns whether this is a dry run. Probably this is helpful for CLI only.
     * @return {@code true} if dry-run mode is on
     */
    public boolean isDryrun() {
        return dryrun;
    }

    /**
     * Set this true to prevent any changes on the triple store. Probably this 
     * is helpful for CLI usage only.
     * @param dryrun 
     */
    public void setDryrun(boolean dryrun) {
        this.dryrun = dryrun;
    }
    
    /**
     * Deletes all data stored in the triplestore (drops all named graphs and
     * cleans the default graph).
     */
    public void deleteAll()
    {
        report("Sending delete command to the triple store.");
        if (!this.dryrun) storage.deleteAll();
        report("Deleted all data from the triplestore.");
    }
    
    /**
     * Delete the data about the DSpaceObject from the triplestore.
     * All data about descendent Subcommunities, Collections and Items will be 
     * deleted as well.
     */
    public void delete(DSpaceObject dso, boolean reset)
            throws SQLException
    {
        if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            throw new IllegalArgumentException(contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                    + " is currently not supported as independent entity.");
        }

        if (dso.getType() == Constants.SITE)
        {
            // we don't need to iterate over all objects, use a shorctut:
            this.deleteAll();
        }
        Callback callback = new Callback() {
            @Override
            protected void callback(DSpaceObject dso)
                    throws SQLException
            {
                String identifier = RDFUtil.generateIdentifier(context, dso);
                
                if (StringUtils.isEmpty(identifier))
                {
                    System.err.println("Cannot determine RDF URI for " 
                            + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID() + "(handle " 
                            + dso.getHandle() + ")" + ", skipping. Please "
                            + "delete it specifying the RDF URI.");
                    log.error("Cannot detgermine RDF URI for " 
                            + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID() + "(handle "
                            + dso.getHandle() + ")" + ", skipping deletion.");
                    return;
                }
                
                report("Deleting Named Graph" + identifier);
                if (!dryrun)
                {
                    storage.delete(identifier);
                }
            }
        };
        this.dspaceDFS(dso, callback, false, reset);
    }
    
    /**
     * Converts and stores all DSpaceObjects that are readable for an anonymous 
     * user.
     */
    public void convertAll()
            throws SQLException
    {
        report("Starting conversion of all DSpaceItems, this may take a while...");
        this.convert(contentServiceFactory.getSiteService().findSite(context), true);
        report("Conversion ended.");
    }
    
    protected void convert(DSpaceObject dso, boolean reset)
            throws SQLException
    {
        if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            throw new IllegalArgumentException(contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                    + " is currently not supported as independent entity.");
        }
        
        Callback callback = new Callback() {
            @Override
            protected void callback(DSpaceObject dso)
                    throws SQLException
            {
                Model converted = null;
                try
                {
                    if (dryrun)
                    {
                        converted = RDFUtil.convert(context, dso);
                    } else {
                        converted = RDFUtil.convertAndStore(context, dso);
                    }
                } catch (ItemNotArchivedException ex) {
                    if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                    report("Skipping conversion of Item " + dso.getID() 
                            + " (handle " + dso.getHandle() + "): Item is not "
                            + "archived.");
                    return;
                } catch (ItemWithdrawnException ex) {
                    if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                    report("Skipping conversion of Item " + dso.getID() 
                            + " (handle " + dso.getHandle() + "): Item is "
                            + "withdrawn.");
                    return;
                } catch (ItemNotDiscoverableException ex) {
                    if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                    report("Skipping conversion of Item " + dso.getID() 
                            + " (handle " + dso.getHandle() + "): Item is not "
                            + "discoverable.");
                    return;
                } catch (AuthorizeException ex) {
                    report("Skipping conversion of " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " 
                            + dso.getID() + " (handle " + dso.getHandle() + ")" 
                            + ", not authorized: " + ex.getMessage());
                    return;
                } catch (RDFMissingIdentifierException ex) {
                    String errormessage = "Skipping conversion of " 
                            + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID() 
                            + " (handle " + dso.getHandle() + ").";
                    log.error(errormessage, ex);
                    System.err.println(errormessage 
                            + " Error while converting: " + ex.getMessage());
                    
                    return;
                }
                    
                if (stdout) {
                    if (converted == null)
                    {
                        System.err.println("Conversion of " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) 
                                + " " + dso.getID() + " resulted in no data.");
                    } else {
                        converted.write(System.out, lang);
                    }
                }
                if (converted != null) converted.close();
            }
        };
        
        this.dspaceDFS(dso, callback, true, reset);
    }
    
    protected void dspaceDFS(DSpaceObject dso, Callback callback, boolean check, boolean reset)
            throws SQLException
    {
        if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            throw new IllegalArgumentException(contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                    + " is currently not supported as independent entity.");
        }

        if (reset)
        {
            this.processed.clear();
        }
        
        if (isProcessed(dso))
        {
            log.debug("Skipping processing of " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " 
                    + dso.getID() + " (handle " + dso.getHandle() 
                    + "), already processed.");
            return;
        }
        markProcessed(dso);
        // this is useful to debug depth first search, but it is really noisy.
        //log.debug("Procesing " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID() + ":" + dso.getHandle() + ".");
        
        // if this method is used for conversion we should check if we have the
        // permissions to read a DSO before converting all of it decendents
        // (e.g. check read permission on a community before converting all of
        // its subcommunties and collections).
        // just skip items with missing permissions and report them.
        if (check)
        {
            try
            {
                RDFUtil.isPublic(context, dso);
            } catch (ItemNotArchivedException ex) {
                if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                report("Skipping processing of Item " + dso.getID() 
                        + " (handle " + dso.getHandle() + "): Item is not "
                        + "archived.");
                return;
            } catch (ItemWithdrawnException ex) {
                if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                report("Skipping processing of Item " + dso.getID() 
                        + " (handle " + dso.getHandle() + "): Item is "
                        + "withdrawn.");
                return;
            } catch (ItemNotDiscoverableException ex) {
                if (!(dso instanceof Item)) throw new IllegalStateException(ex.getMessage(), ex);
                report("Skipping processing of Item " + dso.getID() 
                        + " (handle " + dso.getHandle() + "): Item is not "
                        + "discoverable.");
                return;
            } catch (AuthorizeException ex) {
                report("Skipping processing of " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " 
                        + dso.getID() + " (handle " + dso.getHandle() + ")" 
                        + ", not authorized: " + ex.getMessage());
                return;
            }
        }

        if (dso instanceof Site)
        {
            List<Community> communities = communityService.findAllTop(context);
            for (Community community : communities)
            {
                this.dspaceDFS(community, callback, check, false);
            }

        }
        
        if (dso instanceof Community)
        {
            List<Community> subcommunities = ((Community) dso).getSubcommunities();
            for (Community sub : subcommunities)
            {
                this.dspaceDFS(sub, callback, check, false);
            }
            List<Collection> collections = ((Community) dso).getCollections();
            for (Collection collection : collections)
            {
                this.dspaceDFS(collection, callback, check, false);
            }
        }
        
        if (dso instanceof Collection)
        {
            Iterator<Item> items = itemService.findAllByCollection(context, (Collection) dso);
            while (items.hasNext())
            {
                Item item = items.next();
                this.dspaceDFS(item, callback, check, false);
            }
        }

//        Currently Bundles and Bitstreams aren't supported as independent entities.
//        They should be converted as part of an item. So we do not need to make
//        the recursive call for them. An item itself will be converted as part
//        of the callback call below.
//        The following code is left here for the day, we decide to also convert
//        bundles and/or bitstreams.
//        
//        if (dso instanceof Item)
//        {
//            Bundle[] bundles = ((Item) dso).getBundles();
//            for (Bundle bundle : bundles)
//            {
//                this.dspaceDFS(bundle, callback, check, false);
//            }
//        }
//        
//        if (dso instanceof Bundle)
//        {
//            Bitstream[] bistreams = ((Bundle) dso).getBitstreams();
//            for (Bitstream bitstream : bistreams)
//            {
//                this.dspaceDFS(bitstream, callback, check, false);
//            }
//        }
        
        callback.callback(dso);
        report("Processed " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID()
                + " (handle " + dso.getHandle() + ").");
        context.uncacheEntity(dso);
    }
    
    protected boolean isProcessed(DSpaceObject dso)
    {
        return this.processed.contains(dso.getID());
    }
    
    protected void markProcessed(DSpaceObject dso)
    {
        this.processed.add(dso.getID());
    }
    
    protected void report(String message)
    {
        if (this.verbose)
        {
            System.err.println(message);
        }
        log.debug(message);
    }
    
    protected void runCLI(String[] args)
    {
        // prepare CLI and parse arguments
        Options options = createOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        try 
        {
            line = parser.parse(options, args);
        } 
        catch (ParseException ex)
        {
            usage(options);
            System.err.println();
            System.err.println(ex.getMessage());
            log.fatal(ex);
            System.exit(1);
        }
        
        String[] remainingArgs = line.getArgs();
        if (remainingArgs.length > 0)
        {
            this.usage(options);
            System.err.println();
            StringBuilder builder = new StringBuilder(100);
            for (String argument : remainingArgs)
            {
                if (builder.length() > 0) builder.append(", ");
                builder.append(argument);
            }
            String argumentsLine = builder.toString().trim();
            System.err.print("Cannot recognize the following argument");
            if (remainingArgs.length >= 2) System.err.print("s");
            System.err.println(": " + argumentsLine + ".");
            System.exit(1);
        }


        // set member variables depending on CLI arguments.
        if (line.hasOption("verbose"))
        {
            setVerbose(true);
        }
        
        if (line.hasOption("dry-run"))
        {
            setDryrun(true);
        }

        if (line.hasOption("stdout"))
        {
            setStdout(true);
        }
                
        // check mutual exclusive arguments
        if (line.hasOption("delete") && line.hasOption("delete-all"))
        {
            usage(options);
            System.err.println("\n\nYou cannot use the options --delete <handle> "
                    + "and --delete-all together.");
            System.exit(1);
        }

        if (line.hasOption("convert-all")
                && (line.hasOption("delete") || line.hasOption("delete-all")))
        {
            usage(options);
            System.err.println("\n\nYou cannot use the option --convert-all "
                    + "together with --delete or --delete-all.");
            System.exit(1);
        }
        if (line.hasOption("identifiers")
                && (line.hasOption("delete") || line.hasOption("delete-all")))
        {
            usage(options);
            System.err.println("\n\nYou cannot use the option --identifiers <handle> "
                    + "together with --delete or --delete-all.");
            System.exit(1);
        }
        if (line.hasOption("stdout") 
                && (line.hasOption("delete") || line.hasOption("delete-all")))
        {
            usage(options);
            System.err.println("\n\nYou cannot use the option --stdout together "
                    + "with --delete or --deleta-all.");
            System.exit(1);
        }

        // Run commands depending on CLI arguments.
        // process help first to prevent further evaluation of given options.
        if (line.hasOption('h'))
        {
            usage(options);
            System.exit(0);
        }
        
        if (line.hasOption("delete"))
        {
            String[] identifiers = line.getOptionValues("delete");
            for (String identifier : identifiers)
            {
                if (!StringUtils.startsWithIgnoreCase(identifier, "hdl:"))
                {
                    if (!this.dryrun)
                    {
                        storage.delete(identifier);
                    }
                    if (this.verbose)
                    {
                        System.err.println("Deleted " + identifier + ".");
                    }
                    continue;
                }
                String handle = identifier.substring(4);
                
                log.debug("Trying to resolve identifier " + handle + ".");
                
                DSpaceObject dso = resolveHandle(handle);
                if (dso == null) {
                    // resolveHandle reports problems and return null in case 
                    // of an error or an unresolvable handle.
                    // Don't report it a second time, just continue...
                    continue;
                }
                
                log.debug("Resolved identifier " + handle + " as " 
                        + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " " + dso.getID());
                
                try
                {
                    this.delete(dso, true);
                }
                catch (SQLException ex)
                {
                    log.error(ex);
                    System.err.println("A problem with the database connection "
                            + "occurred. Canceled pending actions.");
                    System.err.println(ex.getMessage());
                    ex.printStackTrace(System.err);
                    System.exit(1);
                }
            }
            System.exit(0);
        }
        
        if (line.hasOption("delete-all"))
        {
            this.deleteAll();
            System.exit(0);
        }
        
        if (line.hasOption("identifiers"))
        {
            String[] identifiers = line.getOptionValues("identifiers");
            report("Starting conversion of specified DSpaceObjects...");
            
            this.processed.clear();
            for (String handle : identifiers)
            {
                log.debug("Trying to resolve identifier " + handle + ".");
                
                DSpaceObject dso = resolveHandle(handle);
                if (dso == null) {
                    // resolveHandle reports problems and return null in case 
                    // of an error or an unresolvable handle.
                    // Don't report it a second time, just continue...
                    continue;
                }
                
                try
                {
                    this.convert(dso, false);
                }
                catch (SQLException ex)
                {
                    log.error(ex);
                    System.err.println("A problem with the database connection "
                            + "occurred. Canceled pending actions.");
                    System.err.println(ex.getMessage());
                    ex.printStackTrace(System.err);
                    System.exit(1);
                }
            }
            report("Conversion ended.");
            System.exit(0);
        }

        if (line.hasOption("convert-all"))
        {
            try {
                this.convertAll();
            }
            catch (SQLException ex)
            {
                log.error(ex);
                System.err.println("A problem with the database connection "
                        + "occurred. Canceled pending actions.");
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                System.exit(1);
            }
            System.exit(0);
        }
        
        this.usage(options);
        System.exit(0);
    }

    protected DSpaceObject resolveHandle(String handle)
    {
        DSpaceObject dso = null;
        try
        {
            dso = handleService.resolveToObject(this.context, handle);
        }
        catch (SQLException ex)
        {
            log.error(ex);
            System.err.println("A problem with the database connection "
                    + "occurred. Canceled pending actions.");
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
        catch (IllegalStateException ex)
        {
            log.error(ex);
            System.err.println("Cannot recognize identifier '" 
                    + handle + "', skipping.");
            return null;
        }
        if (dso == null)
        {
            System.err.println("Cannot resolve identifier '" + handle 
                    + "', skipping.");
            log.debug("Couldn't resolve identifier '" + handle 
                    + "', dso was null.");
            return null;
        }
        if (dso.getType() != Constants.SITE
                && dso.getType() != Constants.COMMUNITY
                && dso.getType() != Constants.COLLECTION
                && dso.getType() != Constants.ITEM)
        {
            System.err.println(contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " are currently not "
                    + "supported as independent entities. Bundles and Bitstreams "
                    + "should be processed as part of their item.");
            return null;
        }

        return dso;
    }
        
    protected Options createOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Print usage information and exit.");
        options.addOption("v", "verbose", false, "Print verbose information to "
                + "stderr while converting data.");
        options.addOption("n", "dry-run", false, "Don't store the converted "
                + "data in the triple store, don't delete data from the "
                + "triplestore. Make a dry run, simulation what would happen.");
        options.addOption("o", "stdout", false, "Print all converted data to " +
                "stdout using turtle as serialization.");
        options.addOption("n", "dry-run", false, "Don't send any data or commands " +
                "to the triplestore. Usefull for debugging or in conjunction " +
                "with --stdout.");
        options.addOption("c", "convert-all", false, "Convert all DSpace Objects" +
                " that are readable for an anonymous user. This may take a long time" +
                "depending on the number of stored communties, collections and " +
                "items. Existing information in the triple store will be updated.");

        Option optIdentifiers = OptionBuilder.withLongOpt("identifiers")
            .hasArgs()
            .withArgName("handle")
            .withValueSeparator(' ')
            .withDescription("Only convert these DSpace Objects. If you specify "
                    + "a Community or Collection all of their Items will be "
                    + "converted as well. Separate multiple identifiers with a "
                    + "space.")
            .create('i');
        options.addOption(optIdentifiers);
        
        Option optDelete = OptionBuilder.withLongOpt("delete")
                .hasArgs()
                .withArgName("hdl:handle | URI")
                .withValueSeparator(' ')
                .withDescription("Delete previously converted data. Specify "
                        + "either the handle of a DSpaceObject in the format "
                        + "'hdl:<handle>' or the URI used to identify the rdf "
                        + "data in the triplestore. If you specify a Community, "
                        + "Collection or Item by its handle all converted "
                        + "information about attached Subcommunities, "
                        + "Collections, Items, Bundles and Bitstreams will be "
                        + "deleted as well. Separate multiple identifiers with "
                        + "a space.")
                .create();
        options.addOption(optDelete);
        
        Option optDeleteAll = OptionBuilder.withLongOpt("delete-all")
                .withDescription("Delete all converted data from the triplestore.")
                .create();
        options.addOption(optDeleteAll);
        
        return options;
    }
    
    protected static void usage(Options options)
    {
        String cliSyntax = "[dspace-bin]/bin/dspace rdfizer [OPTIONS...]";
        String header = "";
        String footer = "\nYou cannot use the options --convert-all, --identifiers " +
                "or --stdout together with --delete or --delete-all.\n" +
                "Please use at least one option out of --convert-all, --delete, " +
                "--delete-all or --identifiers.\n";
        
        PrintWriter err = new PrintWriter(System.err);
        HelpFormatter helpformater = new HelpFormatter();
        helpformater.printHelp(err, 79, cliSyntax, header, options, 2, 2, footer);
        err.flush();
        // don't close PrintWriter err, as it would close System.err!
    }
    
    public static void main(String[] args)
    {
        // get a context from an anonymous user.
        // don't switch off authorization system! We'll export the converted
        // data into a triple store that provides a public sparql endpoint.
        // all exported rdf data can be read by anonymous users.
        // We won't change the database => read_only context will assure this.
        Context context = new Context(Context.Mode.READ_ONLY);

        RDFizer myself = null;
        myself = new RDFizer();        
        myself.overrideContext(context);
        myself.runCLI(args);
        
        // we don't change anything in the database, so abort the context.
        context.abort();
    }
    
    protected abstract class Callback
    {
        protected abstract void callback(DSpaceObject dso)
                throws SQLException;
    }
}
