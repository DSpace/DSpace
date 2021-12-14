/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class DOIConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DOIConsumer.class);

    @Override
    public void initialize() throws Exception {
        // nothing to do
        // we can ask spring to give as a properly setuped instance of
        // DOIIdentifierProvider. Doing so we don't have to configure it and
        // can load it in consume method as this is not very expensive.

    }

    // as we use asynchronous metadata update, our updates are not very expensive.
    // so we can do everything in the consume method.
    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            log.warn("DOIConsumer should not have been given this kind of "
                         + "subject in an event, skipping: " + event.toString());
            return;
        }
        if (Event.MODIFY_METADATA != event.getEventType()) {
            log.warn("DOIConsumer should not have been given this kind of "
                         + "event type, skipping: " + event.toString());
            return;
        }

        DSpaceObject dso = event.getSubject(ctx);
        //FIXME
        if (!(dso instanceof Item)) {
            log.debug("DOIConsumer got an event whose subject was not an item, "
                          + "skipping: " + event.toString());
            return;
        }
        Item item = (Item) dso;

        /*
        if (ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(ctx, item) != null
            || WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(ctx, item) != null) {
            // ignore workflow and workspace items, DOI will be minted when item is installed
            return;
        }
         */
        DOIIdentifierProvider provider = new DSpace().getSingletonService(
            DOIIdentifierProvider.class);

        String doi = null;
        try {
            doi = provider.lookup(ctx, dso);
        } catch (IdentifierNotFoundException ex) {
            // nothing to do here, next if clause will stop us from processing
            // items without dois.
        }
        if (doi == null) {
            // if the item is workflow or workspace, there is a special case here - the ShowIdentifersStep
            // needs this consumer to reserve DOIs in the event that the item now meets criteria for a pre-mint
            if (ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(ctx, item) != null
                    || WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(ctx, item) != null) {
                if (DSpaceServicesFactory.getInstance().getConfigurationService()
                        .getBooleanProperty("identifiers.submission.register", false)) {
                    try {
                        String newDoi = provider.mint(ctx, item);
                        if (newDoi != null) {
                            provider.reserve(ctx, item, newDoi);
                            log.debug("Consumer minting and reserving new DOI for in-progress item: " + newDoi);
                        } else {
                            return;
                        }
                    } catch (DOIIdentifierNotApplicableException e) {
                        log.debug("Consumer skipping mint for item as it was filtered out: " + item.getID());
                    }
                } else {
                    log.debug("Consumer skipping mint for item as it is in-progress and" +
                            "identifiers.submission.register=false: " + item.getID());
                }
            } else {
                log.debug("DOIConsumer cannot handles items without DOIs, skipping: "
                        + event.toString());
                return;
            }
        } else if (ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(ctx, item) != null
                || WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(ctx, item) != null) {
            if (DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getBooleanProperty("identifiers.submission.register", false)) {
                // We have a DOI for an in-progress submission item -- if the filter says "no", we should delete
                // the minted DOI so it doesn't get registered on item install.
                try {
                    provider.checkMintable(ctx, item);
                } catch (DOIIdentifierNotApplicableException e) {
                    log.debug("Consumer deleting DOI for in-progress item: " + doi);
                    provider.delete(ctx, item, doi);
                    return;
                }
            } else {
                log.debug("Consumer skipping mint for item as it is in-progress and" +
                        "identifiers.submission.register=false: " + item.getID());
            }
        }
        try {
            provider.updateMetadata(ctx, dso, doi);
        } catch (IllegalArgumentException ex) {
            // should not happen, as we got the DOI from the DOIProvider
            log.warn("DOIConsumer caught an IdentifierException.", ex);
        } catch (IdentifierException ex) {
            log.warn("DOIConsumer cannot update metadata for Item with ID "
                         + item.getID() + " and DOI " + doi + ".", ex);
        }
    }

    @Override
    public void end(Context ctx) throws Exception {


    }

    @Override
    public void finish(Context ctx) throws Exception {
        // nothing to do
    }

}
