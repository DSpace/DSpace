/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.consumer;

import static java.lang.String.format;
import static org.dspace.app.ldn.LDNMetadataFields.ELEMENT;
import static org.dspace.app.ldn.LDNMetadataFields.RELEASE;
import static org.dspace.app.ldn.LDNMetadataFields.SCHEMA;
import static org.dspace.content.Item.ANY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNBusinessDelegate;
import org.dspace.app.ldn.factory.LDNBusinessDelegateFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Consumer listening for item deposit or update to announce release
 * notification.
 */
public class LDNConsumer implements Consumer {

    private final static Logger log = LogManager.getLogger(LDNConsumer.class);

    private static Set<Item> itemsToRelease;

    private WorkflowItemService<?> workflowItemService;

    private WorkspaceItemService workspaceItemService;

    private ItemService itemService;

    private LDNBusinessDelegate ldnBusinessDelegate;

    /**
     * Initialize all dependencies.
     *
     * @throws Exception
     */
    @Override
    public void initialize() throws Exception {
        workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        ldnBusinessDelegate = LDNBusinessDelegateFactory.getInstance().getLDNBusinessDelegate();
    }

    /**
     * Consume event and determine if release announce is required. Will populate
     * itemsToRelease for those needing to be announced.
     *
     * @param context current context
     * @param event   event consumed
     * @throws Exception something went wrong
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        if (itemsToRelease == null) {
            itemsToRelease = new HashSet<Item>();
        }

        log.info("LDN Release Event consumer consumed {} {}",
                event.getSubjectTypeAsString(), event.getEventTypeAsString());

        int subjectType = event.getSubjectType();
        int eventType = event.getEventType();

        if (subjectType == Constants.ITEM) {

            if (eventType == Event.INSTALL ||
                    eventType == Event.MODIFY_METADATA) {

                Item item = (Item) event.getSubject(context);

                if (item == null) {
                    log.info("Item not found as subject on event");
                    return;
                }

                if (workspaceItemService.findByItem(context, item) != null ||
                        workflowItemService.findByItem(context, item) != null) {
                    log.info("Ignoring item {} as a corresponding workspace or workflow item exists", item.getID());
                    return;
                }

                if (eventType == Event.MODIFY_METADATA) {
                    List<MetadataValue> releaseMetadata = itemService.getMetadata(item, SCHEMA, ELEMENT, RELEASE, ANY);

                    if (!releaseMetadata.isEmpty()) {
                        itemsToRelease.remove(item);
                        log.info("Skipping item {} as it has been notified of release", item.getID());
                        for (MetadataValue metadatum : releaseMetadata) {
                            log.info("\t {}.{}.{} {} {}", SCHEMA, ELEMENT, RELEASE, ANY, metadatum.getValue());
                        }
                        return;
                    }

                }

                List<MetadataValue> researchMetadata = itemService.getMetadata(item, "dc", "data", "uri", ANY);

                if (researchMetadata.isEmpty()) {
                    log.info("Skipping item {} as it has no identifier to notify", item.getID());
                    return;
                }

                for (MetadataValue metadatum : researchMetadata) {
                    if (!item.getMetadata().contains(metadatum)) {
                        item.getMetadata().add(metadatum);
                    }
                }

                if (!itemsToRelease.add(item)) {
                    itemsToRelease.remove(item);
                    itemsToRelease.add(item);
                }

            }
        } else {
            log.warn("Skipping event {} as not an expected type for this consumer", event.toString());
        }

    }

    /**
     * At end of consumer activity, announce all items release.
     *
     * @param context current context
     * @throws Exception failed to announce release
     */
    @Override
    public void end(Context context) throws Exception {
        if (itemsToRelease != null) {
            for (Item item : itemsToRelease) {
                log.info("Item for release {} {}", item.getID(), item.getName());
                try {
                    ldnBusinessDelegate.announceRelease(item);
                } catch (Exception e) {
                    log.error(format(
                        "Failed to announce item %s %s for release",
                        item.getID(),
                        item.getName()
                    ), e);
                }
            }
        }

        itemsToRelease = null;
    }

    /**
     * @param context
     * @throws Exception
     */
    @Override
    public void finish(Context context) throws Exception {
        // nothing to do here
    }

}
