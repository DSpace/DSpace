/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class used to reindex dspace communities/collections/items into discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IndexClient {

    private static final Logger log = Logger.getLogger(IndexClient.class);

    /**
     * Default constructor
     */
    private IndexClient() { }

    /**
     * When invoked as a command-line tool, creates, updates, removes content
     * from the whole index
     *
     * @param args the command-line arguments, none used
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     * @throws IOException            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SearchServiceException if something went wrong with querying the solr server
     */
    public static void main(String[] args) throws SQLException, IOException, SearchServiceException {

        Context context = new Context(Context.Mode.READ_ONLY);
        context.turnOffAuthorisationSystem();

        String usage = "org.dspace.discovery.IndexClient [-cbhf] | [-r <handle>] | [-i <handle>] | [-item_uuid " +
            "<uuid>] or nothing to update/clean an existing index.";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options.addOption(OptionBuilder
                              .withArgName("handle to remove")
                              .hasArg(true)
                              .withDescription(
                                  "remove an Item, Collection or Community from index based on its handle")
                              .create("r"));

        options.addOption(OptionBuilder.withArgName("item uuid to index").hasArg(true)
                .withDescription("add an Item based on its uuid").create("item_uuid"));

        options.addOption(OptionBuilder
                              .withArgName("handle to add or update")
                              .hasArg(true)
                              .withDescription(
                                  "add or update an Item, Collection or Community based on its handle")
                              .create("i"));

        options.addOption(OptionBuilder.isRequired(false).hasArg(true)
                .withDescription("update an Item, Collection or Community from index based on its handle, use with -f "
                        + "to force clean")
                .create("u"));

        options.addOption(OptionBuilder
                              .isRequired(false)
                              .withDescription(
                                  "clean existing index removing any documents that no longer exist in the db")
                              .create("c"));

        options.addOption(OptionBuilder
                              .isRequired(false)
                              .withDescription(
                                  "(re)build index, wiping out current one if it exists")
                              .create("b"));

        options.addOption(OptionBuilder
                              .isRequired(false)
                              .withDescription(
                                  "Rebuild the spellchecker, can be combined with -b and -f.")
                              .create("s"));

        options.addOption(OptionBuilder
                              .isRequired(false)
                              .withDescription(
                                  "if updating existing index, force each handle to be reindexed even if uptodate")
                              .create("f"));

        options.addOption(OptionBuilder
                              .isRequired(false)
                              .withDescription(
                                  "print this help message")
                              .create("h"));

        options.addOption(OptionBuilder.isRequired(false).withDescription(
            "optimize search core").create("o"));

        options.addOption("e", "readfile", true, "Read the identifier from a file");

        try {
            line = new PosixParser().parse(options, args);
        } catch (Exception e) {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        /** Acquire from dspace-services in future */
        /**
         * new DSpace.getServiceManager().getServiceByName("org.dspace.discovery.SolrIndexer");
         */

        IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
            IndexingService.class.getName(),
            IndexingService.class
        );

        if (line.hasOption("r")) {
            log.info("Removing " + line.getOptionValue("r") + " from Index");
            indexer.unIndexContent(context, line.getOptionValue("r"));
        } else if (line.hasOption("c")) {
            log.info("Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
        } else if (line.hasOption("b")) {
            log.info("(Re)building index from scratch.");
            indexer.createIndex(context);
            checkRebuildSpellCheck(line, indexer);
        } else if (line.hasOption("o")) {
            log.info("Optimizing search core.");
            indexer.optimize();
        } else if (line.hasOption('s')) {
            checkRebuildSpellCheck(line, indexer);
        } else if (line.hasOption("item_uuid")) {
            String itemUUID = line.getOptionValue("item_uuid");
            Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString(itemUUID));
            indexer.indexContent(context, item, line.hasOption("f"));
        } else if (line.hasOption("u")) {
            String optionValue = line.getOptionValue("u");
            String[] identifiers = optionValue.split("\\s*,\\s*");
            for (String id : identifiers) {
                if (id.startsWith(ConfigurationManager.getProperty("handle.prefix")) || id.startsWith("123456789/")) {
                    BrowsableDSpaceObject dso = (BrowsableDSpaceObject) HandleServiceFactory.getInstance()
                            .getHandleService().resolveToObject(context, id);
                    indexer.indexContent(context, dso, line.hasOption("f"));
                }
            }
        } else if (line.hasOption('e')) {
            try {
                String filename = line.getOptionValue('e');
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                // Read File Line By Line

                UUID item_id = null;
                List<UUID> ids = new ArrayList<UUID>();

                while ((strLine = br.readLine()) != null) {
                    item_id = UUID.fromString(strLine.trim());
                    ids.add(item_id);
                }

                in.close();

                int type = -1;
                if (line.hasOption('t')) {
                    type = Integer.parseInt(line.getOptionValue("t"));
                } else {
                    // force to item
                    type = Constants.ITEM;
                }
                indexer.updateIndex(context, ids, line.hasOption("f"), type);
            } catch (Exception e) {
                log.error("Error: " + e.getMessage());
            }
        } else if (line.hasOption('i')) {
            final String handle = line.getOptionValue('i');
            final BrowsableDSpaceObject dso = (BrowsableDSpaceObject) HandleServiceFactory.getInstance()
                    .getHandleService().resolveToObject(context, handle);
            if (dso == null) {
                throw new IllegalArgumentException("Cannot resolve " + handle + " to a DSpace object");
            }
            log.info("Forcibly Indexing " + handle);
            final long startTimeMillis = System.currentTimeMillis();
            final long count = indexAll(indexer, ContentServiceFactory.getInstance().getItemService(), context, dso);
            final long seconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
            log.info("Indexed " + count + " DSpace object" + (count > 1 ? "s" : "") + " in " + seconds + " seconds");
        } else {
            log.info("Updating and Cleaning Index");
            indexer.cleanIndex(line.hasOption("f"));
            indexer.updateIndex(context, line.hasOption("f"));
            checkRebuildSpellCheck(line, indexer);
        }

        log.info("Done with indexing");
    }

    /**
     * Indexes the given object and all children, if applicable.
     *
     * @param indexingService
     * @param itemService
     * @param context         The relevant DSpace Context.
     * @param dso             DSpace object to index recursively
     * @throws IOException            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SearchServiceException in case of a solr exception
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     */
    private static long indexAll(final IndexingService indexingService,
                                 final ItemService itemService,
                                 final Context context,
                                 final BrowsableDSpaceObject dso)
        throws IOException, SearchServiceException, SQLException {
        long count = 0;

        indexingService.indexContent(context, dso, true, true);
        count++;
        if (dso.getType() == Constants.COMMUNITY) {
            final Community community = (Community) dso;
            final String communityHandle = community.getHandle();
            for (final Community subcommunity : community.getSubcommunities()) {
                count += indexAll(indexingService, itemService, context, subcommunity);
                //To prevent memory issues, discard an object from the cache after processing
                context.uncacheEntity(subcommunity);
            }
            final Community reloadedCommunity = (Community) HandleServiceFactory.getInstance().getHandleService()
                                                                                .resolveToObject(context,
                                                                                                 communityHandle);
            for (final Collection collection : reloadedCommunity.getCollections()) {
                count++;
                indexingService.indexContent(context, collection, true, true);
                count += indexItems(indexingService, itemService, context, collection);
                //To prevent memory issues, discard an object from the cache after processing
                context.uncacheEntity(collection);
            }
        } else if (dso.getType() == Constants.COLLECTION) {
            count += indexItems(indexingService, itemService, context, (Collection) dso);
        }

        return count;
    }

    /**
     * Indexes all items in the given collection.
     *
     * @param indexingService
     * @param itemService
     * @param context         The relevant DSpace Context.
     * @param collection      collection to index
     * @throws IOException            A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SearchServiceException in case of a solr exception
     * @throws SQLException           An exception that provides information on a database access error or other errors.
     */
    private static long indexItems(final IndexingService indexingService,
                                   final ItemService itemService,
                                   final Context context,
                                   final Collection collection)
        throws IOException, SearchServiceException, SQLException {
        long count = 0;

        final Iterator<Item> itemIterator = itemService.findByCollection(context, collection);
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            indexingService.indexContent(context, item, true, false);
            count++;
            //To prevent memory issues, discard an object from the cache after processing
            context.uncacheEntity(item);
        }
        indexingService.commit();

        return count;
    }

    /**
     * Check the command line options and rebuild the spell check if active.
     *
     * @param line    the command line options
     * @param indexer the solr indexer
     * @throws SearchServiceException in case of a solr exception
     */
    protected static void checkRebuildSpellCheck(CommandLine line, IndexingService indexer)
        throws SearchServiceException {
        if (line.hasOption("s")) {
            log.info("Rebuilding spell checker.");
            indexer.buildSpellCheck();
        }
    }
}
