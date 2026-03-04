/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security.service;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service to check metadata security.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSecurityService {

    /**
     * Returns all the metadata values of the given item filtered by permission
     * evaluations.
     *
     * @param  context the DSpace Context
     * @param  item    the item
     * @return         the metadata values
     */
    List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item);

    /**
     * Returns all the metadata values of the given item filtered by permission
     * evaluations and language preferences.
     * 
     * <p>This method applies the same security filtering as {@link #getPermissionFilteredMetadataValues}
     * but additionally filters metadata by the current locale language from the context.
     * Only metadata values matching the current user's language preference are included
     * in the result.</p>
     *
     * @param  context                 the DSpace Context
     * @param  item                    the item
     * @return                         the metadata values filtered by both permissions and language
     */
    List<MetadataValue> getPermissionAndLangFilteredMetadataFields(Context context, Item item);
}
