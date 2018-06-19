/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;

import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningConsumer implements Consumer {

    private static Set<Item> itemsToProcess;

    private VersionHistoryService versionHistoryService;
    private VersioningService versioningService;
    private ItemService itemService;


    @Override
    public void initialize() throws Exception {
        versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
        versioningService = VersionServiceFactory.getInstance().getVersionService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void finish(Context ctx) throws Exception {}

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if(itemsToProcess == null){
            itemsToProcess = new HashSet<Item>();
        }

        int st = event.getSubjectType();
        int et = event.getEventType();

        if(st == Constants.ITEM && et == Event.INSTALL){
            Item item = (Item) event.getSubject(ctx);
            if (item != null && item.isArchived()) {
                VersionHistory history = versionHistoryService.findByItem(ctx, item);
                if (history != null) {
                    Version latest = versionHistoryService.getLatestVersion(ctx, history);
                    Version previous = versionHistoryService.getPrevious(ctx, history, latest);
                    if(previous != null){
                        Item previousItem = previous.getItem();
                        if(previousItem != null){
                            previousItem.setArchived(false);
                            itemsToProcess.add(previousItem);
                            //Fire a new modify event for our previous item
                            //Due to the need to reindex the item in the search 
                            //and browse index we need to fire a new event
                            ctx.addEvent(new Event(Event.MODIFY, 
                                    previousItem.getType(), previousItem.getID(),
                                    null, itemService.getIdentifiers(ctx, previousItem)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        if(itemsToProcess != null){
            for(Item item : itemsToProcess){
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
