/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.standard.AbstractShellComponent;

/**
 * Discovery commands for the DSpace Spring Shell
 */
@Command(
    command = "discovery",
    group = "Discovery commands"
)
public class DiscoveryCommands extends AbstractShellComponent {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryCommands.class);

    private IndexingService indexer;

    private Context context;

    /**
     * 
     * @param rebuild
     * @param clean
     * @param force
     * @param item
     * @param itemToRemove
     * @param spellchecker
     */
    @Command(
        command = "index",
        alias = "index-discovery",
        description = "called without any options, will update/clean an existing index"
    )
    public void indexDiscovery(
            @Option(
                longNames = "rebuild",
                shortNames = 'b',
                description =
                    "(re)build index, wiping out current one if it exists",
                required = false
            )
            boolean rebuild,
            @Option(
                longNames = "clean",
                shortNames = 'c',
                description =
                    "clean existing index removing any documents that no longer exist in the db",
                required = false
            )
            boolean clean,
            @Option(
                longNames = "force",
                shortNames = 'f',
                description =
                    "if updating existing index, force each handle to be reindexed even if uptodate",
                required = false
            )
            boolean force,
            @Option(
                longNames = "item",
                shortNames = 'i',
                description =
                    "Reindex an individual object (and any child objects).  When run on an Item,"
                     + " it just reindexes that single Item. When run on a Collection, it reindexes"
                     + " the Collection itself and all Items in that Collection. When run on a Community,"
                     + " it reindexes the Community itself and all sub-Communities, contained Collections"
                     + " and contained Items.",
                required = false
            )
            String item,
            @Option(
                longNames = "remove",
                shortNames = 'r',
                description =
                    "Remove an Item, Collection or Community from index based on its handle",
                required = false
            )
            String itemToRemove,
            @Option(
                longNames = "spellchecker",
                shortNames = 's',
                description =
                    "Rebuild the spellchecker, can be combined with -b and -f",
                required = false
            )
            boolean spellchecker
    ) {
        Optional<IndexableObject> indexableObject = Optional.empty();
        try {
            if (item != null && !item.isEmpty()) {
                Context ctx = getContext();
                IndexingService indexingService = getIndexingService();
                System.out.println(
                    "Indexing " + item + " force " + force
                );
                indexableObject = resolveIndexableObject(ctx, item);
                if (!indexableObject.isPresent()) {
                    throw new IllegalArgumentException("Cannot resolve " + item + " to a DSpace object");
                }

                final long startTimeMillis = Instant.now().toEpochMilli();
                final long count = indexAll(indexingService, ContentServiceFactory.getInstance().getItemService(), ctx,
                    indexableObject.get());
                final long seconds = (Instant.now().toEpochMilli() - startTimeMillis) / 1000;
                System.out.println(
                    "Indexed " + count + " object" + (count > 1 ? "s" : "") +
                    " in " + seconds + " seconds"
                );
            }
        } catch (Exception e) {
            System.err.println("Caught exception:");
            e.printStackTrace(System.err);
        }
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
     *  Lazily initialize Context when needed
     * @return context instance
     */
    private Context getContext() {
        if (context == null) {
            context = new Context(Context.Mode.READ_ONLY);
            context.turnOffAuthorisationSystem();
        }
        return context;
    }

    /**
     *  Lazily Use indexing service when needed
     * @return context instance
     */
    private IndexingService getIndexingService () {
        if (indexer == null) {
            indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServiceByName(IndexingService.class.getName(), IndexingService.class);
        }
        return indexer;
    }
}
