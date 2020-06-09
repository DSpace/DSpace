/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.deduplication.service.DedupService;
import org.dspace.app.deduplication.utils.Signature;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

public class DedupEventConsumer implements Consumer {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(DedupEventConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<Item> objectsToUpdate = null;

    private Set<UUID> objectsToDelete = null;

    DSpace dspace = new DSpace();

    DedupService indexer = dspace.getServiceManager().getServiceByName(DedupService.class.getName(),
            DedupService.class);

    Map<UUID, Map<String, List<String>>> cache = new HashMap<UUID, Map<String, List<String>>>();

    Set<String> configuredMetadata = new HashSet<String>();

    public void initialize() throws Exception {
        List<Signature> signAlgo = dspace.getServiceManager().getServicesByType(Signature.class);
        for (Signature algo : signAlgo) {
            configuredMetadata.add(algo.getMetadata());
        }
    }

    /**
     * Consume a content event -- just build the sets of objects to add (new) to the
     * index, update, and delete.
     *
     * @param ctx   DSpace context
     * @param event Content event
     */
    public void consume(Context ctx, Event event) throws Exception {

        if (objectsToUpdate == null) {
            objectsToUpdate = new HashSet<Item>();
            objectsToDelete = new HashSet<UUID>();
        }

        int st = event.getSubjectType();

        DSpaceObject subject = event.getSubject(ctx);
        DSpaceObject object = event.getObject(ctx);

        if (st != Constants.ITEM && st != Constants.BUNDLE) {
            log.warn("IndexConsumer should not have been given this kind of Subject in an event, skipping: "
                    + event.toString());
            return;
        }

        // If event subject is a Bundle and event was Add or Remove,
        // transform the event to be a Modify on the owning Item.
        // It could be a new bitstream in the TEXT bundle which
        // would change the index.
        if (st == Constants.BUNDLE && subject != null) {
            subject = ((Bundle) subject).getItems().get(0);
        }

        int et = event.getEventType();

        switch (et) {
            case Event.CREATE:
            case Event.MODIFY:
            case Event.MODIFY_METADATA:
                if (subject == null) {
                    log.warn(event.getEventTypeAsString() + " event, could not get object for "
                            + event.getSubjectTypeAsString() + " id=" + String.valueOf(event.getSubjectID())
                            + ", perhaps it has been deleted.");
                } else {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    fillObjectToUpdate((Item) subject);
                }
                break;

            case Event.REMOVE:
            case Event.ADD:
                if (subject == null) {
                    log.warn(event.getEventTypeAsString() + " event, could not get object for "
                            + event.getObjectTypeAsString() + " id=" + String.valueOf(event.getObjectID())
                            + ", perhaps it has been deleted.");
                } else {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    fillObjectToUpdate((Item) subject);
                }
                break;

            case Event.DELETE:
                log.debug("consume() adding event to delete queue: " + event.toString());
                objectsToDelete.add(event.getSubjectID());
                break;
            default:
                log.warn("IndexConsumer should not have been given a event of type=" + event.getEventTypeAsString()
                        + " on subject=" + event.getSubjectTypeAsString());
                break;
        }
    }

    private void fillObjectToUpdate(Item subject) {
        if (!cache.containsKey(subject.getID())) {
            objectsToUpdate.add(subject);
            Map<String, List<String>> newCacheMetadata = new HashMap<String, List<String>>();
            boolean saveIt = false;
            for (String mdString : configuredMetadata) {
                List<MetadataValue> values = ContentServiceFactory.getInstance().getDSpaceObjectService(subject)
                        .getMetadataByMetadataString(subject, mdString);
                if (values != null && !values.isEmpty()) {
                    List<String> metadataValue = new ArrayList<String>();
                    for (MetadataValue v : values) {
                        metadataValue.add(v.getValue());
                    }

                    newCacheMetadata.put(mdString, metadataValue);
                    saveIt = true;
                }
            }
            if (saveIt) {
                cache.put(subject.getID(), newCacheMetadata);
            }
        } else {
            // rebuild cache if the lenght of the collections is different or there are a
            // value not contains in the cache
            boolean rebuildCache = false;
            superext: for (String mdString : configuredMetadata) {
                List<String> cachedMdValues = cache.get(subject.getID()).get(mdString);
                List<MetadataValue> mdValues = ContentServiceFactory.getInstance().getDSpaceObjectService(subject)
                        .getMetadataByMetadataString(subject, mdString);

                if (cachedMdValues == null && (mdValues != null && !mdValues.isEmpty())) {
                    rebuildCache = true;
                    break superext;
                }
                if (cachedMdValues != null && mdValues != null) {
                    if (cachedMdValues.size() != mdValues.size()) {
                        rebuildCache = true;
                        break superext;
                    }
                }
                for (MetadataValue mdValue : mdValues) {
                    if (!cachedMdValues.contains(mdValue.getValue())) {
                        rebuildCache = true;
                        break superext;
                    }
                }
            }
            if (rebuildCache) {
                objectsToUpdate.add(subject);
                Map<String, List<String>> newCacheMetadata = new HashMap<String, List<String>>();
                for (String mdString : configuredMetadata) {
                    List<MetadataValue> values = ContentServiceFactory.getInstance().getDSpaceObjectService(subject)
                            .getMetadataByMetadataString(subject, mdString);
                    if (values != null && !values.isEmpty()) {
                        List<String> metadataValue = new ArrayList<String>();
                        for (MetadataValue v : values) {
                            metadataValue.add(v.getValue());
                        }

                        newCacheMetadata.put(mdString, metadataValue);
                    }
                }
                cache.put(subject.getID(), newCacheMetadata);
            }
        }
    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not need
     * to be added or updated, new objects don't also need an update, etc.
     */
    public void end(Context ctx) throws Exception {

        if (objectsToUpdate != null && objectsToDelete != null) {

            // update the changed Items not deleted because they were on create list
            for (Item iu : objectsToUpdate) {
                /*
                 * we let all types through here and allow the search DSIndexer to make
                 * decisions on indexing and/or removal
                 */
                if (!objectsToDelete.contains(iu.getID())) {
                    try {
                        indexer.indexContent(ctx, iu, true);
                        log.debug("Indexed " + Constants.typeText[iu.getType()] + ", id=" + String.valueOf(iu.getID())
                                + ", handle (if exist)=" + iu.getHandle());
                    } catch (Exception e) {
                        log.error("Failed while indexing object: ", e);
                    }
                }
            }

            for (UUID key : objectsToDelete) {
                try {
                    indexer.unIndexContent(ctx, key);
                    if (log.isDebugEnabled()) {
                        log.debug("UN-Indexed ITEM, id=" + key);
                    }
                } catch (Exception e) {
                    log.error("Failed while UN-indexing ITEM id: " + key, e);
                }

            }

            indexer.commit();

        }

        // "free" the resources
        objectsToUpdate = null;
        objectsToDelete = null;
    }

    public void finish(Context ctx) throws Exception {
        // No-op

    }

}
