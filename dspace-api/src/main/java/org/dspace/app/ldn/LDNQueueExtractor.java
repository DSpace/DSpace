/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.factory.LDNMessageServiceFactory;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.core.Context;

public class LDNQueueExtractor {

    private static final LDNMessageService ldnMessageService = LDNMessageServiceFactory.getInstance()
        .getLDNMessageService();
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNQueueExtractor.class);

    /**
     * Default constructor
     */
    private LDNQueueExtractor() {
    }

    public static int extractMessageFromQueue() throws IOException, SQLException {
        log.info("START LDNQueueExtractor.extractMessageFromQueue()");
        Context context = new Context(Context.Mode.READ_WRITE);
        int processed_messages = ldnMessageService.extractAndProcessMessageFromQueue(context);
        if (processed_messages >= 0) {
            log.info("Processed Messages x" + processed_messages);
        } else {
            log.error("Errors happened during the extract operations. Check the log above!");
        }
        context.complete();
        log.info("END LDNQueueExtractor.extractMessageFromQueue()");
        return processed_messages;
    }

};