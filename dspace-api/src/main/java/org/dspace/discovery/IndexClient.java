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
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Class used to reindex dspace communities/collections/items into discovery
 */
public class IndexClient extends DSpaceRunnable<IndexDiscoveryScriptConfiguration> {

    private Context context;
    private IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                               .getServiceByName(IndexingService.class.getName(),
                                                                 IndexingService.class);

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
            indexer.cleanIndex();
        } else if (indexClientOptions == IndexClientOptions.DELETE) {
            handler.logInfo("Deleting Index");
            indexer.deleteIndex();
        } else if (indexClientOptions == IndexClientOptions.BUILD ||
            indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
            handler.logInfo("(Re)building index from scratch.");
            indexer.deleteIndex();
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
            Optional<IndexableObject> indexableObject = Optional.empty();
            if (uuid != null) {
                final Item item = ContentServiceFactory.getInstance().getItemService().find(context, uuid);
                if (item != null) {
                    indexableObject = Optional.of(new IndexableItem(item));
                } else {
                    // it could be a community
                    final Community community = ContentServiceFactory.getInstance().
                            getCommunityService().find(context, uuid);
                    if (community != null) {
                        indexableObject = Optional.of(new IndexableCommunity(community));
                    } else {
                        // it could be a collection
                        final Collection collection = ContentServiceFactory.getInstance().
                                getCollectionService().find(context, uuid);
                        if (collection != null) {
                            indexableObject = Optional.of(new IndexableCollection(collection));
                        }
                    }
                }
            } else {
                final DSpaceObject dso = HandleServiceFactory.getInstance()
                        .getHandleService().resolveToObject(context, param);
                if (dso != null) {
                    final IndexFactory indexableObjectService = IndexObjectFactoryFactory.getInstance().
                            getIndexFactoryByType(String.valueOf(dso.getType()));
                    indexableObject = indexableObjectService.findIndexableObject(context, dso.getID().toString());
                }
            }
            if (!indexableObject.isPresent()) {
                throw new IllegalArgumentException("Cannot resolve " + param + " to a DSpace object");
            }
            handler.logInfo("Indexing " + param + " force " + commandLine.hasOption("f"));
            final long startTimeMillis = System.currentTimeMillis();
            final long count = indexAll(indexer, ContentServiceFactory.getInstance().
                    getItemService(), context, indexableObject.get());
            final long seconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
            handler.logInfo("Indexed " + count + " object" + (count > 1 ? "s" : "") + " in " + seconds + " seconds");
        } else if (indexClientOptions == IndexClientOptions.UPDATE ||
            indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
            handler.logInfo("Updating Index");
            indexer.updateIndex(context, false);
            if (indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        } else if (indexClientOptions == IndexClientOptions.FORCEUPDATE ||
            indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
            handler.logInfo("Updating Index");
            indexer.updateIndex(context, true);
            if (indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
                checkRebuildSpellCheck(commandLine, indexer);
            }
        }

        handler.logInfo("Done with indexing");
    }

    @Override
    public IndexDiscoveryScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("index-discovery",
                                                                 IndexDiscoveryScriptConfiguration.class);
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
        if (dso.getIndexedObject() instanceof Community) {
            final Community community = (Community) dso;
            final String communityHandle = community.getHandle();
            for (final Community subcommunity : community.getSubcommunities()) {
                count += indexAll(indexingService, itemService, context, new IndexableCommunity(subcommunity));
                //To prevent memory issues, discard an object from the cache after processing
                context.uncacheEntity(subcommunity);
            }
            final Community reloadedCommunity = (Community) HandleServiceFactory.getInstance().getHandleService()
                                                                                .resolveToObject(context,
                                                                                                 communityHandle);
            for (final Collection collection : reloadedCommunity.getCollections()) {
                count++;
                indexingService.indexContent(context, new IndexableCollection(collection), true, true);
                count += indexItems(indexingService, itemService, context, collection);
                //To prevent memory issues, discard an object from the cache after processing
                context.uncacheEntity(collection);
            }
        } else if (dso instanceof IndexableCollection) {
            count += indexItems(indexingService, itemService, context, (Collection) dso.getIndexedObject());
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
            indexingService.indexContent(context, new IndexableItem(item), true, false);
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
