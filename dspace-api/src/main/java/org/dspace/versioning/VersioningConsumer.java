/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;

/**
 * When a new version of an item is published, unarchive the previous version and
 * update {@link Relationship#latestVersionStatus} of the relevant relationships.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(VersioningConsumer.class);

    private Set<Item> itemsToProcess;

    private VersionHistoryService versionHistoryService;
    private ItemService itemService;


    @Override
    public void initialize() throws Exception {
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void finish(Context ctx) throws Exception {
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (itemsToProcess == null) {
            itemsToProcess = new HashSet<>();
        }

        // only items
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }

        // only install events
        if (event.getEventType() != Event.INSTALL) {
            return;
        }

        // get the item (should be archived)
        Item item = (Item) event.getSubject(ctx);
        if (item == null || !item.isArchived()) {
            return;
        }

        // get version history
        VersionHistory history = versionHistoryService.findByItem(ctx, item);
        if (history == null) {
            return;
        }

        // get latest version
        Version latestVersion = versionHistoryService.getLatestVersion(ctx, history);
        if (latestVersion == null) {
            return;
        }

        // get previous version
        Version previousVersion = versionHistoryService.getPrevious(ctx, history, latestVersion);
        if (previousVersion == null) {
            return;
        }

        // get latest item
        Item latestItem = latestVersion.getItem();
        if (latestItem == null) {
            String msg = String.format(
                "Illegal state: Obtained version history of item with uuid %s, handle %s, but the latest item is null",
                item.getID(), item.getHandle()
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // get previous item
        Item previousItem = previousVersion.getItem();
        if (previousItem == null) {
            return;
        }

        // unarchive previous item
        unarchiveItem(ctx, previousItem);

        // TODO implement w2p 88061
    }

    protected void unarchiveItem(Context ctx, Item item) {
        item.setArchived(false);
        itemsToProcess.add(item);
        //Fire a new modify event for our previous item
        //Due to the need to reindex the item in the search
        //and browse index we need to fire a new event
        ctx.addEvent(new Event(
            Event.MODIFY, item.getType(), item.getID(), null, itemService.getIdentifiers(ctx, item)
        ));
    }

    @Override
    public void end(Context ctx) throws Exception {
        if (itemsToProcess != null) {
            for (Item item : itemsToProcess) {
                ctx.turnOffAuthorisationSystem();
                try {
                    itemService.update(ctx, item);
                } finally {
                    ctx.restoreAuthSystemState();
                }
            }
        }

        itemsToProcess = null;
    }

}
