/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.discovery.IndexClientOptions.TYPE_OPTION;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
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
 * Class used to reindex DSpace communities/collections/items into discovery.
 */
public class IndexClient extends DSpaceRunnable<IndexDiscoveryScriptConfiguration> {

    private Context context;
    private IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    private IndexClientOptions indexClientOptions;

    @Override
    public void internalRun() throws Exception {
        if (indexClientOptions == IndexClientOptions.HELP) {
            printHelp();
            return;
        }

        String type = null;
        if (commandLine.hasOption(TYPE_OPTION)) {
            List<String> indexableObjectTypes = IndexObjectFactoryFactory.getInstance().getIndexFactories().stream()
                    .map((indexFactory -> indexFactory.getType())).collect(Collectors.toList());
            type = commandLine.getOptionValue(TYPE_OPTION);
            if (!indexableObjectTypes.contains(type)) {
                handler.handleException(String.format("%s is not a valid indexable object type, options: %s",
                        type, Arrays.toString(indexableObjectTypes.toArray())));
            }
        }

        Optional<IndexableObject> indexableObject = Optional.empty();

        if (indexClientOptions == IndexClientOptions.REMOVE || indexClientOptions == IndexClientOptions.INDEX) {
            final String param = indexClientOptions == IndexClientOptions.REMOVE ? commandLine.getOptionValue('r')
                    : commandLine.getOptionValue('i');
            indexableObject = resolveIndexableObject(context, param);
            if (!indexableObject.isPresent()) {
                throw new IllegalArgumentException("Cannot resolve " + param + " to a DSpace object");
            }
        }

        switch (indexClientOptions) {
            case REMOVE:
                handler.logInfo("Removing " + commandLine.getOptionValue("r") + " from Index");
                indexer.unIndexContent(context, indexableObject.get().getUniqueIndexID());
                break;
            case CLEAN:
                handler.logInfo("Cleaning Index");
                indexer.cleanIndex();
                break;
            case DELETE:
                handler.logInfo("Deleting Index");
                indexer.deleteIndex();
                break;
            case BUILD:
            case BUILDANDSPELLCHECK:
                handler.logInfo("(Re)building index from scratch.");
                if (StringUtils.isNotBlank(type)) {
                    handler.logWarning(String.format(
                            "Type option, %s, not applicable for entire index rebuild option, b"
                                    + ", type will be ignored",
                            TYPE_OPTION));
                }
                indexer.deleteIndex();
                indexer.createIndex(context);
                if (indexClientOptions == IndexClientOptions.BUILDANDSPELLCHECK) {
                    checkRebuildSpellCheck(commandLine, indexer);
                }
                break;
            case OPTIMIZE:
                handler.logInfo("Optimizing search core.");
                indexer.optimize();
                break;
            case SPELLCHECK:
                checkRebuildSpellCheck(commandLine, indexer);
                break;
            case INDEX:
                handler.logInfo("Indexing " + commandLine.getOptionValue('i') + " force " + commandLine.hasOption("f"));
                final long startTimeMillis = Instant.now().toEpochMilli();
                final long count = indexAll(indexer, ContentServiceFactory.getInstance().getItemService(), context,
                    indexableObject.get());
                final long seconds = (Instant.now().toEpochMilli() - startTimeMillis) / 1000;
                handler.logInfo("Indexed " + count + " object" + (count > 1 ? "s" : "") +
                                " in " + seconds + " seconds");
                break;
            case UPDATE:
            case UPDATEANDSPELLCHECK:
                handler.logInfo("Updating Index");
                indexer.updateIndex(context, false, type);
                if (indexClientOptions == IndexClientOptions.UPDATEANDSPELLCHECK) {
                    checkRebuildSpellCheck(commandLine, indexer);
                }
                break;
            case FORCEUPDATE:
            case FORCEUPDATEANDSPELLCHECK:
                handler.logInfo("Updating Index");
                indexer.updateIndex(context, true, type);
                if (indexClientOptions == IndexClientOptions.FORCEUPDATEANDSPELLCHECK) {
                    checkRebuildSpellCheck(commandLine, indexer);
                }
                break;
            default:
                handler.handleException("Invalid index client option.");
                break;
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
     * Resolves the given parameter to an IndexableObject (Item, Collection, or Community).
     *
     * @param context The relevant DSpace Context.
     * @param param   The UUID or handle of the DSpace object.
     * @return An Optional containing the IndexableObject if found.
     * @throws SQLException If database error occurs.
     */
    private Optional<IndexableObject> resolveIndexableObject(Context context, String param) throws SQLException {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(param);
        } catch (Exception e) {
            // It's not a UUID, proceed to treat it as a handle.
        }

        if (uuid != null) {
            Item item = ContentServiceFactory.getInstance().getItemService().find(context, uuid);
            if (item != null) {
                return Optional.of(new IndexableItem(item));
            }
            Community community = ContentServiceFactory.getInstance().getCommunityService().find(context, uuid);
            if (community != null) {
                return Optional.of(new IndexableCommunity(community));
            }
            Collection collection = ContentServiceFactory.getInstance().getCollectionService().find(context, uuid);
            if (collection != null) {
                return Optional.of(new IndexableCollection(collection));
            }
        } else {
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, param);
            if (dso != null) {
                IndexFactory indexableObjectService = IndexObjectFactoryFactory.getInstance()
                        .getIndexFactoryByType(Constants.typeText[dso.getType()]);
                return indexableObjectService.findIndexableObject(context, dso.getID().toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Indexes the given object and all its children recursively.
     *
     * @param indexingService The indexing service.
     * @param itemService     The item service.
     * @param context         The relevant DSpace Context.
     * @param indexableObject The IndexableObject to index recursively.
     * @return The count of indexed objects.
     * @throws IOException            If I/O error occurs.
     * @throws SearchServiceException If a search service error occurs.
     * @throws SQLException           If database error occurs.
     */
    private long indexAll(final IndexingService indexingService, final ItemService itemService, final Context context,
            final IndexableObject indexableObject) throws IOException, SearchServiceException, SQLException {
        long count = 0;

        boolean commit = indexableObject instanceof IndexableCommunity ||
            indexableObject instanceof IndexableCollection;
        indexingService.indexContent(context, indexableObject, true, commit);
        count++;

        if (indexableObject instanceof IndexableCommunity) {
            final Community community = (Community) indexableObject.getIndexedObject();
            final String communityHandle = community.getHandle();
            for (final Community subcommunity : community.getSubcommunities()) {
                count += indexAll(indexingService, itemService, context, new IndexableCommunity(subcommunity));
                context.uncacheEntity(subcommunity);
            }
            // Reload community to get up-to-date collections
            final Community reloadedCommunity = (Community) HandleServiceFactory.getInstance().getHandleService()
                    .resolveToObject(context, communityHandle);
            for (final Collection collection : reloadedCommunity.getCollections()) {
                count += indexAll(indexingService, itemService, context, new IndexableCollection(collection));
                context.uncacheEntity(collection);
            }
        } else if (indexableObject instanceof IndexableCollection) {
            final Collection collection = (Collection) indexableObject.getIndexedObject();
            final Iterator<Item> itemIterator = itemService.findByCollection(context, collection);
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                indexingService.indexContent(context, new IndexableItem(item), true, false);
                count++;
                context.uncacheEntity(item);
            }
            indexingService.commit();
        }

        return count;
    }

    /**
     * Check the command line options and rebuild the spell check if active.
     *
     * @param line    the command line options
     * @param indexer the solr indexer
     * @throws SearchServiceException in case of a solr exception
     * @throws IOException            If I/O error occurs.
     */
    protected void checkRebuildSpellCheck(CommandLine line, IndexingService indexer)
            throws SearchServiceException, IOException {
        handler.logInfo("Rebuilding spell checker.");
        indexer.buildSpellCheck();
    }

}
