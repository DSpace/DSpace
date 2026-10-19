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
 * This class contains business-logic to handle {@link RegistrationDataMetadata}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface RegistrationDataMetadataService extends DSpaceCRUDService<RegistrationDataMetadata> {

    /**
     * Creates a new {@link RegistrationDataMetadata} that will be stored starting from the parameters of the method.
     *
     * @param context - the DSpace Context
     * @param registrationData - the Registration data that will contain the metadata
     * @param schema - the schema of the metadata field
     * @param element - the element of the metadata field
     * @param qualifier - the qualifier of the metadata field
     * @param value - the value of that metadata
     * @return the newly created RegistrationDataMetadata
     * @throws SQLException
     */
    RegistrationDataMetadata create(Context context, RegistrationData registrationData, String schema,
                                    String element, String qualifier, String value) throws SQLException;

    /**
     * Creates a new {@link RegistrationDataMetadata}
     *
     * @param context - the DSpace Context
     * @param registrationData - the RegistrationData that will contain that metadata
     * @param metadataField - the metadataField
     * @return the newly created RegistrationDataMetadata
     * @throws SQLException
     */
    RegistrationDataMetadata create(
        Context context, RegistrationData registrationData, MetadataField metadataField
    ) throws SQLException;

    /**
     * Creates a new {@link RegistrationDataMetadata}
     *
     * @param context - the DSpace Context
     * @param registrationData - the RegistrationData that will contain that metadata
     * @param metadataField - the metadataField that will be stored
     * @param value - the value that will be placed inside the RegistrationDataMetadata
     * @return the newly created {@link RegistrationDataMetadata}
     * @throws SQLException
     */
    RegistrationDataMetadata create(
        Context context, RegistrationData registrationData, MetadataField metadataField, String value
    ) throws SQLException;
}
