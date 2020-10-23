/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface NBAction {
    public void applyCorrection(Context context, Item item, Item relatedItem, MessageDto message);
}
