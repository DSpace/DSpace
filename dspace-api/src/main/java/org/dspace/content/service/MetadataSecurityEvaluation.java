/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;

/**
 * Handles the show of the metadata values of the item based on a metadata
 * field.
 *
 * @author Alba Aliu
 */
public interface MetadataSecurityEvaluation {

    /**
     * @param  context       The context of the app
     * @param  item          The Item for which the user wants to see the metadata
     * @param  metadataField The metadata field related with a metadata value
     *
     * @return               true/false if the user can/'t see the metadata
     */
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException;

}
