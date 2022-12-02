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
import org.dspace.content.Bitstream;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.core.Context;

public interface ClarinLicenseResourceMappingService {

    ClarinLicenseResourceMapping create(Context context) throws SQLException, AuthorizeException;
    ClarinLicenseResourceMapping create(Context context, ClarinLicenseResourceMapping clarinLicenseResourceMapping)
            throws SQLException, AuthorizeException;
    ClarinLicenseResourceMapping create(Context context, Integer licenseId, UUID bitstreamUuid)
            throws SQLException, AuthorizeException;

    ClarinLicenseResourceMapping find(Context context, int valueId) throws SQLException;
    List<ClarinLicenseResourceMapping> findAll(Context context) throws SQLException;
    List<ClarinLicenseResourceMapping> findAllByLicenseId(Context context, Integer licenseId) throws SQLException;

    void update(Context context, ClarinLicenseResourceMapping newClarinLicenseResourceMapping) throws SQLException;

    void delete(Context context, ClarinLicenseResourceMapping clarinLicenseResourceMapping) throws SQLException;

    void detachLicenses(Context context, Bitstream bitstream) throws SQLException;

    void attachLicense(Context context, ClarinLicense clarinLicense, Bitstream bitstream)
            throws SQLException, AuthorizeException;

    List<ClarinLicenseResourceMapping> findByBitstreamUUID(Context context, UUID bitstreamID) throws SQLException;

    ClarinLicense getLicenseToAgree(Context context, UUID userId, UUID resourceID) throws SQLException;
}
