/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.clarin;

import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ClarinItemDAO {
    List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException;
}
