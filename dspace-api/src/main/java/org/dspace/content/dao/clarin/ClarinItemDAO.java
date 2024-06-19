/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;

public interface ClarinItemDAO {
    List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException;

    List<Item> findByHandle(Context context, MetadataField metadataField, String handle) throws SQLException;
}
