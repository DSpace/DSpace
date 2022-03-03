/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

public class LDNReleaseEventConsumer implements Consumer {

    private final static Logger log = LogManager.getLogger(LDNReleaseEventConsumer.class);

    @Override
    public void initialize() throws Exception {
        log.info("LDN Release Event consumer started");
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        log.info("LDN Release Event consumer consumed {} {}",
                event.getObjectTypeAsString(), event.getEventTypeAsString());
    }

    @Override
    public void end(Context ctx) throws Exception {
        log.info("LDN Release Event consumer ended");
    }

    @Override
    public void finish(Context ctx) throws Exception {
        log.info("LDN Release Event consumer finished");
    }

}
