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

public interface LDNAction {

    public ActionStatus execute(Notification notification, Item item) throws Exception;

}
