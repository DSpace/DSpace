/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;

public class LDNEmailAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(LDNEmailAction.class);

    @Override
    public boolean execute(Notification notification) throws Exception {
        log.info("Email action");
        return true;
    }

}
