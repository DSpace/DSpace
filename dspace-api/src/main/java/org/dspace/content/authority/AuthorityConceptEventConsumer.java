/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

import java.util.HashSet;
import java.util.Set;

/**
 * Class for updating indices in SolrAuthority when Concepts Change.
 *
 * @author Mark Diggory (markd at atmire dot com)
 * @author Lantian Gai (lantian at atmire dot com)
 */
public class AuthorityConceptEventConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityConceptEventConsumer.class);

    // collect Concepts that need indexing
    private Set<Concept> conceptsToUpdate = null;

    // identifiers to delete since IDs are not useful by now.
    private Set<String> identifiersToDelete = null;

    DSpace dspace = new DSpace();

    EditableAuthorityIndexingService indexer = dspace.getServiceManager().getServiceByName(EditableAuthorityIndexingService.class.getName(),EditableAuthorityIndexingService.class);

    public void initialize() throws Exception {

    }

    /**
     * Consume a content event -- just build the sets of objects to add (new) to
     * the index, update, and delete.
     *
     * @param ctx   DSpace context
     * @param event Content event
     */
    public void consume(Context ctx, Event event) throws Exception {

        if (conceptsToUpdate == null) {
            conceptsToUpdate = new HashSet<Concept>();
            identifiersToDelete = new HashSet<String>();
        }

        int st = event.getSubjectType();
        if (st == Constants.CONCEPT || st == Constants.TERM) {

            AuthorityObject subject = (AuthorityObject)AuthorityObject.find(ctx,event.getSubjectType(),event.getSubjectID());

            AuthorityObject object = (AuthorityObject)event.getObject(ctx);

            int et = event.getEventType();

            // If subject is a TERM, we will just Event.MODIFY its Constants.CONCEPT.
            if (st == Constants.TERM) {
                if ((et == Event.MODIFY) && subject != null) {
                    st = Constants.CONCEPT;
                    et = Event.MODIFY;
                    subject = ((Term) subject).getConcepts()[0]; /* there should really only be one */
                    if (log.isDebugEnabled())
                    {
                        log.debug("Transforming Term event into MODIFY of Concept "
                                + subject.getIdentifier());
                    }
                } else
                {
                    return;
                }
            }


            switch (et) {
                case Event.CREATE:
                case Event.MODIFY:
                case Event.MODIFY_METADATA:
                    if (subject == null)
                    {
                        log.warn(event.getEventTypeAsString() + " event, could not get subject for "
                                + event.getSubjectTypeAsString() + " id="
                                + String.valueOf(event.getSubjectID())
                                + ", perhaps it has been deleted.");
                    }
                    else if(subject instanceof Concept) {
                        if(Concept.Status.ACCEPTED.name().equals(((Concept) subject).getStatus()))
                        {
                            log.debug("consume() adding event to update queue: " + event.toString());
                            /** Index Accepted Concepts */
                            conceptsToUpdate.add((Concept) subject);
                        }
                        else
                        {
                            /** Don't index Candidate or withdrawn Concepts */
                            identifiersToDelete.add(((Concept) subject).getIdentifier());
                        }
                    }
                    break;

                case Event.REMOVE:
                case Event.ADD:
                    if (object == null)
                    {
                        log.warn(event.getEventTypeAsString() + " event, could not get object for "
                                + event.getObjectTypeAsString() + " id="
                                + String.valueOf(event.getObjectID())
                                + ", perhaps it has been deleted.");
                    }
                    else if(object instanceof Concept) {
                        log.debug("consume() adding event to update queue: " + event.toString());
                        conceptsToUpdate.add((Concept) object);
                    }
                    break;

                case Event.DELETE:
                    String detail = event.getDetail();
                    if (detail == null)
                    {
                        log.warn("got null detail on DELETE event, skipping it.");
                    }
                    else {
                        log.debug("consume() adding event to delete queue: " + event.toString());
                        identifiersToDelete.add(detail);
                    }
                    break;
                default:
                    log.warn("IndexConsumer should not have been given a event of type="
                                    + event.getEventTypeAsString()
                                    + " on subject="
                                    + event.getSubjectTypeAsString());
                    break;
            }

        }

    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
    public void end(Context ctx) throws Exception {

        if (conceptsToUpdate != null && identifiersToDelete != null) {

            for (Concept concept : conceptsToUpdate) {
                String identifier = concept.getIdentifier();
                if (identifier != null && !identifiersToDelete.contains(identifier)) {
                    try {
                        indexer.indexContent(ctx,concept, true);
                        log.debug("Indexed "
                                + Constants.typeText[concept.getType()]
                                + ", id=" + String.valueOf(concept.getID())
                                + ", identifier=" + identifier);
                    }
                    catch (Exception e) {
                        log.error("Failed while indexing object: ", e);
                    }
                }
            }

            for (String identifier : identifiersToDelete) {
                try {
                    indexer.unIndexContent(ctx, identifier, true);
                    if (log.isDebugEnabled())
                    {
                        log.debug("UN-Indexed Concept, identifier=" + identifier);
                    }
                }
                catch (Exception e) {
                    log.error("Failed while UN-indexing object: " + identifier, e);
                }

            }

        }
        indexer.commit();
        // "free" the resources
        conceptsToUpdate = null;
        identifiersToDelete = null;
    }

    public void finish(Context ctx) throws Exception {
        // No-op

    }


}
