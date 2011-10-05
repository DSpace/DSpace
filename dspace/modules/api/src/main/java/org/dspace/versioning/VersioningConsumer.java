package org.dspace.versioning;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 16, 2011
 * Time: 1:26:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersioningConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(VersioningConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<DSpaceObject> objectsToUpdate = null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception {
        // No-op

    }

//    /**
//     * Consume a content event -- just build the sets of objects to add (new) to
//     * the index, update, and delete.
//     *
//     * @param ctx   DSpace context
//     * @param event Content event
//     */
//    public void consume(Context ctx, Event event) throws Exception {
//
//        if (objectsToUpdate == null) {
//            objectsToUpdate = new HashSet<DSpaceObject>();
//            handlesToDelete = new HashSet<String>();
//        }
//
//        int st = event.getSubjectType();
//        if (!(st == Constants.ITEM || st == Constants.BUNDLE
//                || st == Constants.COLLECTION || st == Constants.COMMUNITY)) {
//            log
//                    .warn("Consumer should not have been given this kind of Subject in an event, skipping: "
//                            + event.toString());
//            return;
//        }
//
//        DSpaceObject subject = event.getSubject(ctx);
//
//        DSpaceObject object = event.getObject(ctx);
//
//
//        // If event subject is a Bundle and event was Add or Remove,
//        // transform the event to be a Modify on the owning Item.
//        // It could be a new bitstream in the TEXT bundle which
//        // would change the index.
//        int et = event.getEventType();
//        if (st == Constants.BUNDLE) {
//            if ((et == Event.ADD || et == Event.REMOVE) && subject != null
//                    && "ORIGINAL".equals(((Bundle) subject).getName())) {
//                st = Constants.ITEM;
//                et = Event.MODIFY;
//                subject = ((Bundle) subject).getItems()[0];
//                if (log.isDebugEnabled()) {
//                    log.debug("Transforming Bundle event into MODIFY of Item "
//                            + subject.getHandle());
//                }
//            } else {
//                return;
//            }
//        }
//
//        switch (et) {
//            case Event.CREATE:
//            case Event.MODIFY:
//            case Event.MODIFY_METADATA:
//                if (subject == null) {
//                    log.warn(event.getEventTypeAsString() + " event, could not get object for "
//                            + event.getSubjectTypeAsString() + " id="
//                            + String.valueOf(event.getSubjectID())
//                            + ", perhaps it has been deleted.");
//                } else {
//                    log.debug("consume() adding event to update queue: " + event.toString());
//                    objectsToUpdate.add(subject);
//                }
//                break;
//
//            case Event.REMOVE:
//            case Event.ADD:
//                if (object == null) {
//                    log.warn(event.getEventTypeAsString() + " event, could not get object for "
//                            + event.getObjectTypeAsString() + " id="
//                            + String.valueOf(event.getObjectID())
//                            + ", perhaps it has been deleted.");
//                } else {
//                    log.debug("consume() adding event to update queue: " + event.toString());
//                    objectsToUpdate.add(object);
//                }
//                break;
//
//            case Event.DELETE:
//                String detail = event.getDetail();
//                if (detail == null) {
//                    log.warn("got null detail on DELETE event, skipping it.");
//                } else {
//                    log.debug("consume() adding event to delete queue: " + event.toString());
//                    handlesToDelete.add(detail);
//                }
//                break;
//            default:
//                log
//                        .warn("Consumer should not have been given a event of type="
//                                + event.getEventTypeAsString()
//                                + " on subject="
//                                + event.getSubjectTypeAsString());
//                break;
//        }
//    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
//    public void end(Context ctx) throws Exception {
//
//        if (objectsToUpdate != null && handlesToDelete != null) {
//
//            // update the changed Items not deleted because they were on create list
//            for (DSpaceObject iu : objectsToUpdate) {
//                /* we let all types through here and
//                 * allow the search DSIndexer to make
//                 * decisions on indexing and/or removal
//                 */
//                String hdl = iu.getHandle();
//                if (hdl != null && !handlesToDelete.contains(hdl)) {
//                    try {
//
//                        update(iu);
//
//                    } catch (Exception e) {
//                        log.error("Failed while indexing object: ", e);
//                    }
//                }
//            }
//
//            for (String hdl : handlesToDelete) {
//                try {
//
//                    delete(hdl);
//
//                } catch (Exception e) {
//                    log.error("Failed while UN-indexing object: " + hdl, e);
//                }
//
//            }
//
//        }
//
//        // "free" the resources
//        objectsToUpdate = null;
//        handlesToDelete = null;
//    }
    public void finish(Context ctx) throws Exception {
        // No-op
    }

    public void end(Context ctx) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    //public void update(Context ctx, Event event) throws Exception{

    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();
                 
        try {
            ctx = new Context();
            ctx.turnOffAuthorisationSystem();
            switch (st) {

                // Versioning: when the new version is archived, set previousVersion.inArchive=false(if there is one.)
                case Constants.ITEM: {
                    if (et == Event.MODIFY) {
                        Item item = (Item) event.getSubject(ctx);
                        if (item != null && item.isArchived()) {
                            setArchived(ctx, item);
                            Item[] items = org.dspace.workflow.DryadWorkflowUtils.getDataFiles(ctx, item);
                            for (Item datafile : items) {
                                setArchived(ctx, datafile);
                            }
                            ctx.commit();


                        }
                    }
                    break;
                }
                case Constants.BUNDLE: {
                    if (et == Event.ADD || et == Event.REMOVE) {
                        Bundle b = (Bundle) event.getSubject(ctx);
                        Item item = (Item) b.getParentObject();
                        if(item.isArchived()){
                            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
                            identifierService.register(ctx, item);                            
                        }
                    }

                }
                break;
            }
        }
        catch (Exception e) {
            ctx.abort();
        }
        finally {
            ctx.complete();
        }

    }


    private static org.dspace.versioning.VersionHistory retrieveVersionHistory(Context c, Item item) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(c, item.getID());

        return history;
    }

    private void setArchived(Context ctx, Item item) throws AuthorizeException, SQLException {

        VersionHistory history = retrieveVersionHistory(ctx, item);
        if (history != null) {
            Version latest = history.getLatestVersion();
            Version previous = history.getPrevious(latest);
            previous.getItem().setArchived(false);
            previous.getItem().update();
        }
    }


}
