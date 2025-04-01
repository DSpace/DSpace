/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * An action that is run after a notification has been processed.
 */
public interface LDNAction {

    /**
     * Execute action for provided notification and item corresponding to the
     * notification context.
     *
     *@param context  the context
     * @param notification the processed notification to perform action against
     * @param item         the item corresponding to the notification context
     * @return ActionStatus the resulting status of the action
     * @throws Exception general exception that can be thrown while executing action
     */
    public LDNActionStatus execute(Context context, Notification notification, Item item) throws Exception;

}