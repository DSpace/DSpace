/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.converter;

import org.dspace.content.dto.ItemDTO;
import org.dspace.core.Context;

/**
 * Converter between a generic source and an {@link ItemDTO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 * @param  <T> the type of the source to be converted
 */
public interface ItemDTOConverter<T> {

    /**
     * Convert the given source to an instance of {@link ItemDTO}.
     *
     * @param  context the DSpace context
     * @param  source  the source to be converted
     * @return         the ItemDTO instance
     */
    ItemDTO convert(Context context, T source);
}
