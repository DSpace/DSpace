/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class used to reindex dspace communities/collections/items into discovery
 */
public class IndexClient extends DSpaceRunnable {

    private Context context;

    @Autowired
    private IndexingService indexer;

    private IndexClientOptions indexClientOptions;

    @Override
    public void internalRun() throws Exception {
        if (indexClientOptions == IndexClientOptions.HELP) {
            printHelp();
            return;
        }

        /** Acquire from dspace-services in future */
        /**
         * new DSpace.getServiceManager().getServiceByName("org.dspace.discovery.SolrIndexer");
         */

        if (indexClientOptions == IndexClientOptions.REMOVE) {
            handler.logInfo("Removing " + commandLine.getOptionValue("r") + " from Index");
            indexer.unIndexContent(context, commandLine.getOptionValue("r"));
        } else if (indexClientOptions == IndexClientOptions.CLEAN) {
            handler.logInfo("Cleaning Index");
            indexer.cleanIndex(false);
        } else if (indexClientOptions == IndexClientOptions.FORCECLEAN) {
            handler.logInfo("Cleaning Index");
            indexer.cleanIndex(true);
        } else if (indexClientOptions == IndexClientOptions.BUILD ||
            indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
            handler.logInfo("(Re)building index from scratch.");
            indexer.createIndex(context);
            if (indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        } else if (indexClientOptions == IndexClientOptions.OPTIMIZE) {
            handler.logInfo("Optimizing search core.");
            indexer.optimize();
        } else if (indexClientOptions == IndexClientOptions.SPELLCHECK) {
            checkRebuildSpellCheck(commandLine, indexer);
        } else if (indexClientOptions == IndexClientOptions.INDEX) {
            final String param = commandLine.getOptionValue('i');
            UUID uuid = null;
            try {
                uuid = UUID.fromString(param);
            } catch (Exception e) {
                // nothing to do, it should be an handle
            }
            IndexableObject dso = null;
            if (uuid != null) {
                dso = ContentServiceFactory.getInstance().getItemService().find(context, uuid);
                if (dso == null) {
                    // it could be a community
                    dso = ContentServiceFactory.getInstance().getCommunityService().find(context, uuid);
                    if (dso == null) {
                        // it could be a collection
                        dso = ContentServiceFactory.getInstance().getCollectionService().find(context, uuid);
                    }
                }
            } else {
                dso = (IndexableObject) HandleServiceFactory.getInstance()
                                                            .getHandleService().resolveToObject(context, param);
            }
            if (dso == null) {
                throw new IllegalArgumentException("Cannot resolve " + param + " to a DSpace object");
            }
            handler.logInfo("Indexing " + param + " force " + commandLine.hasOption("f"));
            final long startTimeMillis = System.currentTimeMillis();
            final long count = indexAll(indexer, ContentServiceFactory.getInstance().getItemService(), context,
                                        dso);
            final long seconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
            handler.logInfo("Indexed " + count + " object" + (count > 1 ? "s" : "") + " in " + seconds + " seconds");
        } else if (indexClientOptions == IndexClientOptions.UPDATE ||
            indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
            handler.logInfo("Updating and Cleaning Index");
            indexer.cleanIndex(false);
            indexer.updateIndex(context, false);
            if (indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        } else if (indexClientOptions == IndexClientOptions.FORCEUPDATE ||
            indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
            handler.logInfo("Updating and Cleaning Index");
            indexer.cleanIndex(true);
            indexer.updateIndex(context, true);
            if (indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        }

        handler.logInfo("Done with indexing");
    }

    public void setup() throws ParseException {
        try {
            context = new Context(Context.Mode.READ_ONLY);
            context.turnOffAuthorisationSystem();
        } catch (Exception e) {
            throw new ParseException("Unable to create a new DSpace Context: " + e.getMessage());
        }

        indexClientOptions = IndexClientOptions.getIndexClientOption(commandLine);
    }

    /**
     * Constructor for this class. This will ensure that the Options are created and set appropriately.
     */
    private IndexClient() {
        Options options = IndexClientOptions.constructOptions();
        this.options = options;
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
                                 final IndexableObject dso)
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
     * @throws IOException passed through
     */
    protected void checkRebuildSpellCheck(CommandLine line, IndexingService indexer)
        throws SearchServiceException, IOException {
        handler.logInfo("Rebuilding spell checker.");
        indexer.buildSpellCheck();
    }

}
