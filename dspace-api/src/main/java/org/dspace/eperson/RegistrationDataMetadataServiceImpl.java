/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.dao.RegistrationDataMetadataDAO;
import org.dspace.eperson.service.RegistrationDataMetadataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationDataMetadataServiceImpl implements RegistrationDataMetadataService {

    @Autowired
    private RegistrationDataMetadataDAO registrationDataMetadataDAO;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public RegistrationDataMetadata create(Context context, RegistrationData registrationData, String schema,
                                           String element, String qualifier, String value) throws SQLException {
        return create(
            context, registrationData,
            metadataFieldService.findByElement(context, schema, element, qualifier),
            value
        );
    }

    @Override
    public RegistrationDataMetadata create(Context context, RegistrationData registrationData,
                                           MetadataField metadataField) throws SQLException {
        RegistrationDataMetadata metadata = new RegistrationDataMetadata();
        metadata.setRegistrationData(registrationData);
        metadata.setMetadataField(metadataField);
        return registrationDataMetadataDAO.create(context, metadata);
    }

    @Override
    public RegistrationDataMetadata create(
        Context context, RegistrationData registrationData, MetadataField metadataField, String value
    ) throws SQLException {
        RegistrationDataMetadata metadata = new RegistrationDataMetadata();
        metadata.setRegistrationData(registrationData);
        metadata.setMetadataField(metadataField);
        metadata.setValue(value);
        return registrationDataMetadataDAO.create(context, metadata);
    }

    @Override
    public RegistrationDataMetadata create(Context context) throws SQLException, AuthorizeException {
        return registrationDataMetadataDAO.create(context, new RegistrationDataMetadata());
    }

    @Override
    public RegistrationDataMetadata find(Context context, int id) throws SQLException {
        return registrationDataMetadataDAO.findByID(context, RegistrationData.class, id);
    }

    @Override
    public void update(Context context, RegistrationDataMetadata registrationDataMetadata)
        throws SQLException, AuthorizeException {
        registrationDataMetadataDAO.save(context, registrationDataMetadata);
    }

    @Override
    public void update(Context context, List<RegistrationDataMetadata> t) throws SQLException, AuthorizeException {
        for (RegistrationDataMetadata registrationDataMetadata : t) {
            update(context, registrationDataMetadata);
        }
    }

    @Override
    public void delete(Context context, RegistrationDataMetadata registrationDataMetadata)
        throws SQLException, AuthorizeException {
        registrationDataMetadataDAO.delete(context, registrationDataMetadata);
    }
}
