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

import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface ClarinUserMetadataDAO extends GenericDAO<ClarinUserMetadata> {
    List<ClarinUserMetadata> findByUserRegistrationAndBitstream(Context context, Integer userRegUUID,
                                                                UUID bitstreamUUID) throws SQLException;
}
