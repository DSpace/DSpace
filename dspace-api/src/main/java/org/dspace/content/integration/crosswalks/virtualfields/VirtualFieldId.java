/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Implementation of {@link VirtualField} that returns the item uuid.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldId implements VirtualField {

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        return new String[] { item.getID().toString() };
    }

}
