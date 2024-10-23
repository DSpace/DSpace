/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.sql.SQLException;

import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationDataMetadata;
import org.dspace.service.DSpaceCRUDService;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface RegistrationDataMetadataService extends DSpaceCRUDService<RegistrationDataMetadata> {

    RegistrationDataMetadata create(Context context, RegistrationData registrationData, String schema,
                                    String element, String qualifier, String value) throws SQLException;

    RegistrationDataMetadata create(
        Context context, RegistrationData registrationData, MetadataField metadataField
    ) throws SQLException;

    RegistrationDataMetadata create(
        Context context, RegistrationData registrationData, MetadataField metadataField, String value
    ) throws SQLException;
}
