/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationDataMetadata;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.dto.RegistrationDataPatch;
import org.dspace.service.DSpaceCRUDService;

/**
 * Service interface class for the {@link RegistrationData} object.
 * The implementation of this class is responsible for all business logic calls for the RegistrationData object and
 * is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RegistrationDataService extends DSpaceCRUDService<RegistrationData> {

    RegistrationData create(Context context) throws SQLException, AuthorizeException;

    RegistrationData create(Context context, String netId) throws SQLException, AuthorizeException;

    RegistrationData create(Context context, String netId, RegistrationTypeEnum type)
        throws SQLException, AuthorizeException;

    RegistrationData clone(
        Context context, RegistrationDataPatch registrationDataPatch
    ) throws SQLException, AuthorizeException;

    public RegistrationData findByToken(Context context, String token) throws SQLException;

    public RegistrationData findByEmail(Context context, String email) throws SQLException;

    RegistrationData findBy(Context context, String email, RegistrationTypeEnum type) throws SQLException;

    public void deleteByToken(Context context, String token) throws SQLException;

    Stream<Map.Entry<RegistrationDataMetadata, Optional<MetadataValue>>> groupEpersonMetadataByRegistrationData(
        EPerson ePerson, RegistrationData registrationData
    ) throws SQLException;

    void setRegistrationMetadataValue(
        Context context, RegistrationData registration, String schema, String element, String qualifier, String value
    ) throws SQLException, AuthorizeException;

    void addMetadata(
        Context context, RegistrationData registration, String schema, String element, String qualifier, String value
    ) throws SQLException, AuthorizeException;

    RegistrationDataMetadata getMetadataByMetadataString(RegistrationData registrationData, String field);

    void addMetadata(Context context, RegistrationData rd, MetadataField metadataField, String value)
        throws SQLException, AuthorizeException;

    void markAsExpired(Context context, RegistrationData registrationData) throws SQLException, AuthorizeException;

    void deleteExpiredRegistrations(Context context) throws SQLException;

    boolean isValid(RegistrationData rd);
}
