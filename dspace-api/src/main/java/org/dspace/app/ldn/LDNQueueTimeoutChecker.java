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

public class LDNQueueTimeoutChecker {

    private static final LDNMessageService ldnMessageService = LDNMessageServiceFactory.getInstance()
        .getLDNMessageService();
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNQueueTimeoutChecker.class);

    /**
     * Default constructor
     */
    private LDNQueueTimeoutChecker() {
    }

    public static int checkQueueMessageTimeout() throws IOException, SQLException {
        log.info("START LDNQueueTimeoutChecker.checkQueueMessageTimeout()");
        Context context = new Context(Context.Mode.READ_WRITE);
        int fixed_messages = 0;
        fixed_messages = ldnMessageService.checkQueueMessageTimeout(context);
        if (fixed_messages >= 0) {
            log.info("Managed Messages x" + fixed_messages);
        } else {
            log.error("Errors happened during the check operation. Check the log above!");
        }
        log.info("END LDNQueueTimeoutChecker.checkQueueMessageTimeout()");
        return fixed_messages;
    }
}