/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.consumer;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.factory.LDNBusinessDelegateFactory;
import org.dspace.app.ldn.service.LDNBusinessDelegate;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * @author Stefano Maffei (steph-ieffam @ 4Science)
 *
 */
public class LDNOfferConsumer implements Consumer {

    private final static Logger log = LogManager.getLogger(LDNAnnounceConsumer.class);

    private static Set<Item> items;

    private LDNBusinessDelegate ldnBusinessDelegate;

    /**
     * Initialize all dependencies.
     *
     * @throws Exception
     */
    @Override
    public void initialize() throws Exception {
        ldnBusinessDelegate = LDNBusinessDelegateFactory.getInstance().getLDNBusinessDelegate();
        if (items == null) {
            items = new HashSet<Item>();
        }
    }

    /**
     * Consume event and determine if release announce is required. Will populate
     * itemsToRelease for those needing to be announced.
     *
     * @param  context   current context
     * @param  event     event consumed
     * @throws Exception something went wrong
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception {

        log.info("LDN Offer Review Event consumer consumed {} {}",
            event.getSubjectTypeAsString(), event.getEventTypeAsString());

        int subjectType = event.getSubjectType();
        int eventType = event.getEventType();

        if (subjectType == Constants.ITEM) {

            if (eventType == Event.MODIFY_METADATA ||
                eventType == Event.INSTALL) {

                Item item = (Item) event.getSubject(ctx);

                if (item == null) {
                    log.info("Item not found as subject on event");
                    return;
                }
                Set<String> endpoints = LDNUtils.getMetadataLdnInitialize(item);

                // Check to prevent from handling item without proper metadata
                if (CollectionUtils.isNotEmpty(endpoints)) {
                    items.add(item);
                }
            }
        }
    }

    /**
     * At end of consumer activity, announce all items release.
     *
     * @param  context   current context
     * @throws Exception if failed to request review
     */
    @Override
    public void end(Context ctx) throws Exception {
        items.forEach(item -> {
            try {
                ldnBusinessDelegate.handleRequest("Offer:ReviewAction", ctx, item);
            } catch (Exception e) {
                log.error("An error occurred while asking review for item {}", item.getID());
            }
        });
        items.clear();
    }

    /**
     * @param  context
     * @throws Exception
     */
    @Override
    public void finish(Context ctx) throws Exception {

    }
}
