/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import org.dspace.app.nbevent.service.dto.NBMessageDTO;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes that perform a correction on the given item.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface NBAction {

    /**
     * Perform a correction on the given item.
     *
     * @param context     the DSpace context
     * @param item        the item to correct
     * @param relatedItem the related item, if any
     * @param message     the message with the correction details
     */
    public void applyCorrection(Context context, Item item, Item relatedItem, NBMessageDTO message);
}
