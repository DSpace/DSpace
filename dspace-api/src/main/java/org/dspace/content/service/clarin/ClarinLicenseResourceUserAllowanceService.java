/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.core.Context;

public interface ClarinLicenseResourceUserAllowanceService {
    ClarinLicenseResourceUserAllowance create(Context context) throws SQLException;
    ClarinLicenseResourceUserAllowance find(Context context, int valueId) throws SQLException;
    List<ClarinLicenseResourceUserAllowance> findAll(Context context) throws SQLException, AuthorizeException;
    void update(Context context, ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance)
            throws SQLException;
    void delete(Context context, ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance)
            throws SQLException, AuthorizeException;
    boolean verifyToken(Context context, UUID resourceID, String token) throws SQLException;
    boolean isUserAllowedToAccessTheResource(Context context, UUID userId, UUID resourceId) throws SQLException;
    List<ClarinLicenseResourceUserAllowance> findByEPersonId(Context context, UUID userID) throws SQLException;
    List<ClarinLicenseResourceUserAllowance> findByEPersonIdAndBitstreamId(Context context, UUID userID, UUID bitstreamID) throws SQLException;
}
