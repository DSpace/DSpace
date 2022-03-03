/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.consumer;

import static org.dspace.app.ldn.LDNMetadataFields.ELEMENT;
import static org.dspace.app.ldn.LDNMetadataFields.RELEASE;
import static org.dspace.app.ldn.LDNMetadataFields.SCHEMA;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNBusinessDelegate;
import org.dspace.app.ldn.factory.LDNBusinessDelegateFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
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

public class LDNReleaseConsumer implements Consumer {

    private final static Logger log = LogManager.getLogger(LDNReleaseConsumer.class);

    private static Set<Item> itemsToRelease;

    private WorkflowItemService workflowItemService;

    private WorkspaceItemService workspaceItemService;

    private ItemService itemService;

    private LDNBusinessDelegate ldnBusinessDelegate;

    @Override
    public void initialize() throws Exception {
        workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        itemService = ContentServiceFactory.getInstance().getItemService();

        log.info("\n\n" + LDNBusinessDelegateFactory.getInstance() + "\n\n");

        if (LDNBusinessDelegateFactory.getInstance() != null) {
            ldnBusinessDelegate = LDNBusinessDelegateFactory.getInstance().getLDNBusinessDelegate();
            log.info("\n\n" + ldnBusinessDelegate + "\n\n");
        }
        
    }

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
                    List<MetadataValue> releaseMetadata = itemService.getMetadata(
                            item,
                            SCHEMA,
                            ELEMENT,
                            RELEASE,
                            Item.ANY);

                    if (!releaseMetadata.isEmpty()) {
                        itemsToRelease.remove(item);
                        log.info("Skipping item {} as it has been notified of release", item.getID());
                        for (MetadataValue metadatum : releaseMetadata) {
                            log.info("\t {}.{}.{} {} {}", SCHEMA, ELEMENT, RELEASE, Item.ANY, metadatum.getValue());
                        }
                        return;
                    }

                }

                List<MetadataValue> researchMetadata = itemService.getMetadata(
                        item,
                        "dc",
                        "data",
                        "uri",
                        Item.ANY);

                if (researchMetadata.isEmpty()) {
                    log.info("Skipping item {} as it has no identifier to notify", item.getID());
                    return;
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

    @Override
    public void end(Context context) throws Exception {
        if (itemsToRelease != null) {
            for (Item item : itemsToRelease) {
                List<MetadataValue> metadata = item.getMetadata();

                log.info("Item for release  {}", item.getID());
                for (MetadataValue value : metadata) {
                    MetadataField field = value.getMetadataField();
                    log.info("Metadata field {} with value {}", field, value.getValue());
                }

            }
        }

        itemsToRelease = null;
    }

    @Override
    public void finish(Context context) throws Exception {

    }

}
