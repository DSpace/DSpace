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

import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface ClarinLicenseResourceUserAllowanceDAO extends GenericDAO<ClarinLicenseResourceUserAllowance> {
    List<ClarinLicenseResourceUserAllowance> findByTokenAndBitstreamId(Context context, UUID resourceID,
                                                                       String token) throws SQLException;
    List<ClarinLicenseResourceUserAllowance> findByEPersonId(Context context, UUID userID) throws SQLException;
    List<ClarinLicenseResourceUserAllowance> findByEPersonIdAndBitstreamId(Context context, UUID userID,
                                                                           UUID bitstreamID) throws SQLException;
}
