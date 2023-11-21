/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import org.dspace.app.ldn.model.Notification;
import org.dspace.core.Context;

/**
 * Processor interface to allow for custom implementations of process.
 */
public interface LDNProcessor {

    /**
     * Process received notification.
     *
     * @param notification received notification
     * @throws Exception something went wrong processing the notification
     */
    public void process(Context context, Notification notification) throws Exception;
}