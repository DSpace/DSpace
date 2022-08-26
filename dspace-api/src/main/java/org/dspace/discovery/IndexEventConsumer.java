/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class for updating search indices in discovery from content events.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IndexEventConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(IndexEventConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<IndexableObject> objectsToUpdate = new HashSet<>();
    // collect freshly created Items that need indexing (requires pre-db status)
    private Set<IndexableObject> createdItemsToUpdate = new HashSet<>();

    // unique search IDs to delete
    private Set<String> uniqueIdsToDelete = new HashSet<>();

    IndexingService indexer = DSpaceServicesFactory.getInstance().getServiceManager()
                                                   .getServiceByName(IndexingService.class.getName(),
                                                                     IndexingService.class);

    IndexObjectFactoryFactory indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();

    @Override
    public void initialize() throws Exception {

    }

    /**
     * Consume a content event -- just build the sets of objects to add (new) to
     * the index, update, and delete.
     *
     * @param ctx   DSpace context
     * @param event Content event
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception {

        if (objectsToUpdate == null) {
            objectsToUpdate = new HashSet<>();
            uniqueIdsToDelete = new HashSet<>();
            createdItemsToUpdate = new HashSet<>();
        }

        int st = event.getSubjectType();
        if (!(st == Constants.ITEM || st == Constants.BUNDLE
            || st == Constants.COLLECTION || st == Constants.COMMUNITY || st == Constants.SITE)) {
            log
                .warn("IndexConsumer should not have been given this kind of Subject in an event, skipping: "
                          + event.toString());
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);

        DSpaceObject object = event.getObject(ctx);


        // If event subject is a Bundle and event was Add or Remove,
        // transform the event to be a Modify on the owning Item.
        // It could be a new bitstream in the TEXT bundle which
        // would change the index.
        int et = event.getEventType();
        if (st == Constants.BUNDLE) {
            if ((et == Event.ADD || et == Event.REMOVE) && subject != null
                && ((Bundle) subject).getName().equals("TEXT")) {
                st = Constants.ITEM;
                et = Event.MODIFY;
                subject = ((Bundle) subject).getItems().get(0);
                if (log.isDebugEnabled()) {
                    log.debug("Transforming Bundle event into MODIFY of Item "
                                  + subject.getHandle());
                }
            } else {
                return;
            }
        }

        switch (et) {
            case Event.CREATE:
            case Event.MODIFY:
            case Event.MODIFY_METADATA:
                if (subject == null) {
                    if (st == Constants.SITE) {
                        // Update the indexable objects of type in event.detail of objects with ids in event.identifiers
                        for (String id : event.getIdentifiers()) {
                            IndexFactory indexableObjectService = IndexObjectFactoryFactory.getInstance().
                                getIndexFactoryByType(event.getDetail());
                            Optional<IndexableObject> indexableObject = Optional.empty();
                            indexableObject = indexableObjectService.findIndexableObject(ctx, id);
                            if (indexableObject.isPresent()) {
                                log.debug("consume() adding event to update queue: " + event.toString());
                                objectsToUpdate
                                    .addAll(indexObjectServiceFactory
                                        .getIndexableObjects(ctx, indexableObject.get().getIndexedObject()));
                            } else {
                                log.warn("Cannot resolve " + id);
                            }
                        }
                    } else {
                        log.warn(event.getEventTypeAsString() + " event, could not get object for "
                                 + event.getSubjectTypeAsString() + " id="
                                 + event.getSubjectID()
                                 + ", perhaps it has been deleted.");
                    }
                } else {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    if (event.getSubjectType() == Constants.ITEM) {
                    // if it is an item we cannot know about its previous state, so it could be a
                    // workspaceitem that has been deposited right now or an approved/reject
                    // workflowitem.
                    // As the workflow is not necessary enabled it can happen than a workspaceitem
                    // became directly an item without giving us the chance to retrieve a
                    // workflowitem... so we need to force the unindex of all the related data
                    // before to index it again to be sure to don't leave any zombie in solr
                        IndexFactory indexableObjectService = IndexObjectFactoryFactory.getInstance()
                                              .getIndexFactoryByType(Constants.typeText[event.getSubjectType()]);
                        String detail = indexableObjectService.getType() + "-" + event.getSubjectID().toString();
                        uniqueIdsToDelete.add(detail);
                    }

                    objectsToUpdate.addAll(indexObjectServiceFactory.getIndexableObjects(ctx, subject));
                }
                break;

            case Event.REMOVE:
            case Event.ADD:
                if (object == null) {
                    log.warn(event.getEventTypeAsString() + " event, could not get object for "
                                 + event.getObjectTypeAsString() + " id="
                                 + event.getObjectID()
                                 + ", perhaps it has been deleted.");
                } else {
                    log.debug("consume() adding event to update queue: " + event.toString());
                    objectsToUpdate.addAll(indexObjectServiceFactory.getIndexableObjects(ctx, subject));

                    // If the event subject is a Collection and the event object is an Item,
                    // also update the object in order to index mapped/unmapped Items
                    if (subject != null &&
                        subject.getType() == Constants.COLLECTION && object.getType() == Constants.ITEM) {
                        createdItemsToUpdate.addAll(indexObjectServiceFactory.getIndexableObjects(ctx, object));
                    }
                }
                break;

            case Event.DELETE:
                if (event.getSubjectType() == -1 || event.getSubjectID() == null) {
                    log.warn("got null subject type and/or ID on DELETE event, skipping it.");
                } else {
                    IndexFactory indexableObjectService = IndexObjectFactoryFactory.getInstance()
                                          .getIndexFactoryByType(Constants.typeText[event.getSubjectType()]);
                    String detail = indexableObjectService.getType() + "-" + event.getSubjectID().toString();
                    log.debug("consume() adding event to delete queue: " + event.toString());
                    uniqueIdsToDelete.add(detail);
                }
                break;
            default:
                log
                    .warn("IndexConsumer should not have been given a event of type="
                              + event.getEventTypeAsString()
                              + " on subject="
                              + event.getSubjectTypeAsString());
                break;
        }
    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
    @Override
    public void end(Context ctx) throws Exception {

        try {
            for (String uid : uniqueIdsToDelete) {
                try {
                    indexer.unIndexContent(ctx, uid, false);
                    if (log.isDebugEnabled()) {
                        log.debug("UN-Indexed Item, handle=" + uid);
                    }
                } catch (Exception e) {
                    log.error("Failed while UN-indexing object: " + uid, e);
                }
            }
            // update the changed Items not deleted because they were on create list
            for (IndexableObject iu : objectsToUpdate) {
                indexObject(ctx, iu, false);
            }
            // update the created Items with a pre-db status
            for (IndexableObject iu : createdItemsToUpdate) {
                indexObject(ctx, iu, true);
            }
        } finally {
            if (!objectsToUpdate.isEmpty() || !uniqueIdsToDelete.isEmpty()) {

                indexer.commit();

                // "free" the resources
                objectsToUpdate.clear();
                uniqueIdsToDelete.clear();
                createdItemsToUpdate.clear();
            }
        }
    }

    private void indexObject(Context ctx, IndexableObject iu, boolean preDb) throws SQLException {
        /* we let all types through here and
         * allow the search indexer to make
         * decisions on indexing and/or removal
         */
        iu.setIndexedObject(ctx.reloadEntity(iu.getIndexedObject()));
        String uniqueIndexID = iu.getUniqueIndexID();
        if (uniqueIndexID != null) {
            try {
                indexer.indexContent(ctx, iu, true, false, preDb);
                log.debug("Indexed "
                        + iu.getTypeText()
                        + ", id=" + iu.getID()
                        + ", unique_id=" + uniqueIndexID);
            } catch (Exception e) {
                log.error("Failed while indexing object: ", e);
            }
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // No-op

    }

}
