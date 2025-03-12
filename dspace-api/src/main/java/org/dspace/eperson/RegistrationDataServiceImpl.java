/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.eperson.dto.RegistrationDataChanges;
import org.dspace.eperson.dto.RegistrationDataPatch;
import org.dspace.eperson.service.RegistrationDataMetadataService;
import org.dspace.eperson.service.RegistrationDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the RegistrationData object.
 * This class is responsible for all business logic calls for the RegistrationData object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RegistrationDataServiceImpl implements RegistrationDataService {
    @Autowired()
    protected RegistrationDataDAO registrationDataDAO;

    @Autowired()
    protected RegistrationDataMetadataService registrationDataMetadataService;

    @Autowired()
    protected MetadataFieldService metadataFieldService;

    protected RegistrationDataExpirationConfiguration expirationConfiguration =
        RegistrationDataExpirationConfiguration.getInstance();

    protected RegistrationDataServiceImpl() {

    }

    @Override
    public RegistrationData create(Context context) throws SQLException, AuthorizeException {
        return create(context, null, null);
    }


    @Override
    public RegistrationData create(Context context, String netId) throws SQLException, AuthorizeException {
        return this.create(context, netId, null);
    }

    @Override
    public RegistrationData create(Context context, String netId, RegistrationTypeEnum type)
        throws SQLException, AuthorizeException {
        return registrationDataDAO.create(context, newInstance(netId, type, null));
    }

    private RegistrationData newInstance(String netId, RegistrationTypeEnum type, String email) {
        RegistrationData rd = new RegistrationData(netId);
        rd.setToken(Utils.generateHexKey());
        rd.setRegistrationType(type);
        rd.setExpires(expirationConfiguration.computeExpirationDate(type));
        rd.setEmail(email);
        return rd;
    }

    @Override
    public RegistrationData clone(
        Context context, RegistrationDataPatch registrationDataPatch
    ) throws SQLException, AuthorizeException {
        RegistrationData old = registrationDataPatch.getOldRegistration();
        RegistrationDataChanges changes = registrationDataPatch.getChanges();
        RegistrationData rd = newInstance(old.getNetId(), changes.getRegistrationType(), changes.getEmail());

        for (RegistrationDataMetadata metadata : old.getMetadata()) {
            addMetadata(context, rd, metadata.getMetadataField(), metadata.getValue());
        }

        return registrationDataDAO.create(context, rd);
    }

    private boolean isEmailConfirmed(RegistrationData old, String newEmail) {
        return newEmail.equals(old.getEmail());
    }

    @Override
    public RegistrationData findByToken(Context context, String token) throws SQLException {
        return registrationDataDAO.findByToken(context, token);
    }

    @Override
    public RegistrationData findByEmail(Context context, String email) throws SQLException {
        return registrationDataDAO.findByEmail(context, email);
    }

    @Override
    public RegistrationData findBy(Context context, String email, RegistrationTypeEnum type) throws SQLException {
        return registrationDataDAO.findBy(context, email, type);
    }

    @Override
    public void deleteByToken(Context context, String token) throws SQLException {
        registrationDataDAO.deleteByToken(context, token);

    }

    @Override
    public Stream<Map.Entry<RegistrationDataMetadata, Optional<MetadataValue>>> groupEpersonMetadataByRegistrationData(
        EPerson ePerson, RegistrationData registrationData
    )
        throws SQLException {
        Map<MetadataField, List<MetadataValue>> epersonMeta =
            ePerson.getMetadata()
                   .stream()
                   .collect(
                       Collectors.groupingBy(
                           MetadataValue::getMetadataField
                       )
                   );
        return registrationData.getMetadata()
                               .stream()
                               .map(meta ->
                                        Map.entry(
                                            meta,
                                            Optional.ofNullable(epersonMeta.get(meta.getMetadataField()))
                                                    .filter(list -> list.size() == 1)
                                                    .map(values -> values.get(0))
                                        )
                               );
    }

    @Override
    public void setRegistrationMetadataValue(
        Context context, RegistrationData registration, String schema, String element, String qualifier, String value
    ) throws SQLException, AuthorizeException {

        List<RegistrationDataMetadata> metadata =
            registration.getMetadata()
                        .stream()
                        .filter(m -> areEquals(m, schema, element, qualifier))
                        .collect(Collectors.toList());

        if (metadata.size() > 1) {
            throw new IllegalStateException("Find more than one registration metadata to update!");
        }

        RegistrationDataMetadata registrationDataMetadata;
        if (metadata.isEmpty()) {
            registrationDataMetadata =
                createMetadata(context, registration, schema, element, qualifier, value);
        } else {
            registrationDataMetadata = metadata.get(0);
            registrationDataMetadata.setValue(value);
        }
        registrationDataMetadataService.update(context, registrationDataMetadata);
    }

    @Override
    public void addMetadata(
        Context context, RegistrationData registration, MetadataField mf, String value
    ) throws SQLException, AuthorizeException {
        registration.getMetadata().add(
            registrationDataMetadataService.create(context, registration, mf, value)
        );
        this.update(context, registration);
    }

    @Override
    public void addMetadata(
        Context context, RegistrationData registration, String schema, String element, String qualifier, String value
    ) throws SQLException, AuthorizeException {
        MetadataField mf = metadataFieldService.findByElement(context, schema, element, qualifier);
        registration.getMetadata().add(
            registrationDataMetadataService.create(context, registration, mf, value)
        );
        this.update(context, registration);
    }

    @Override
    public RegistrationDataMetadata getMetadataByMetadataString(RegistrationData registrationData, String field) {
        return registrationData.getMetadata().stream()
                               .filter(m -> field.equals(m.getMetadataField().toString('.')))
                               .findFirst().orElse(null);
    }

    private boolean areEquals(RegistrationDataMetadata m, String schema, String element, String qualifier) {
        return m.getMetadataField().getMetadataSchema().getName().equals(schema)
            && m.getMetadataField().getElement().equals(element)
            && StringUtils.equals(m.getMetadataField().getQualifier(), qualifier);
    }

    private RegistrationDataMetadata createMetadata(
        Context context, RegistrationData registration,
        String schema, String element, String qualifier,
        String value
    ) {
        try {
            return registrationDataMetadataService.create(
                context, registration, schema, element, qualifier, value
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RegistrationDataMetadata createMetadata(Context context, RegistrationData registration, MetadataField mf) {
        try {
            return registrationDataMetadataService.create(context, registration, mf);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RegistrationData find(Context context, int id) throws SQLException {
        return registrationDataDAO.findByID(context, RegistrationData.class, id);
    }

    @Override
    public void update(Context context, RegistrationData registrationData) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(registrationData));
    }

    @Override
    public void update(Context context, List<RegistrationData> registrationDataRecords)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(registrationDataRecords)) {
            for (RegistrationData registrationData : registrationDataRecords) {
                registrationDataDAO.save(context, registrationData);
            }
        }
    }

    @Override
    public void markAsExpired(Context context, RegistrationData registrationData) throws SQLException {
        registrationData.setExpires(Instant.now());
        registrationDataDAO.save(context, registrationData);
    }

    @Override
    public void delete(Context context, RegistrationData registrationData) throws SQLException, AuthorizeException {
        registrationDataDAO.delete(context, registrationData);
    }

    @Override
    public void deleteExpiredRegistrations(Context context) throws SQLException {
        registrationDataDAO.deleteExpiredBy(context, Instant.now());
    }

    @Override
    public boolean isValid(RegistrationData rd) {
        return rd.getExpires() == null || rd.getExpires().isAfter(Instant.now());
    }

}
