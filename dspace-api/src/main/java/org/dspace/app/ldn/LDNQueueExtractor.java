/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.factory.LDNMessageServiceFactory;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.core.Context;

/**
 * LDN Message manager: scheduled task invoking extractAndProcessMessageFromQueue() of {@link LDNMessageService}
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
public class LDNQueueExtractor {

    private static final LDNMessageService ldnMessageService = LDNMessageServiceFactory.getInstance()
        .getLDNMessageService();
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNQueueExtractor.class);

    /**
     * Default constructor
     */
    private LDNQueueExtractor() {
    }

    /**
     * invokes
     * @see org.dspace.app.ldn.service.impl.LDNMessageServiceImpl#extractAndProcessMessageFromQueue(Context)
     * to process the oldest ldn messages from the queue. An LdnMessage is processed when is routed to a
     * @see org.dspace.app.ldn.processor.LDNProcessor
     * Also a +1 is added to the ldnMessage entity
     * @see org.dspace.app.ldn.LDNMessageEntity#getQueueAttempts()
     * @return the number of processed ldnMessages.
     * @throws SQLException
     */
    public static int extractMessageFromQueue() throws SQLException {
        Context context = new Context(Context.Mode.READ_WRITE);
        int processed_messages = ldnMessageService.extractAndProcessMessageFromQueue(context);
        if (processed_messages > 0) {
            log.info("Processed Messages x" + processed_messages);
        }
        context.complete();
        return processed_messages;
    }

};