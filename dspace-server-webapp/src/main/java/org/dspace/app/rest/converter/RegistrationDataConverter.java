/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.RegistrationMetadataRest;
import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.RegistrationDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts a given {@link RegistrationRest} DTO into a {@link RegistrationData} entity.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Component
public class RegistrationDataConverter implements DSpaceConverter<RegistrationData, RegistrationRest> {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Override
    public RegistrationRest convert(RegistrationData registrationData, Projection projection) {

        if (registrationData == null) {
            return null;
        }

        Context context = ContextUtil.obtainContext(request);

        AccountService accountService = EPersonServiceFactory.getInstance().getAccountService();
        RegistrationRest registrationRest = new RegistrationRest();
        registrationRest.setId(registrationData.getID());
        registrationRest.setEmail(registrationData.getEmail());
        registrationRest.setNetId(registrationData.getNetId());
        registrationRest.setRegistrationType(
            Optional.ofNullable(registrationData.getRegistrationType())
                    .map(RegistrationTypeEnum::toString)
                    .orElse(null)
        );

        EPerson ePerson = null;
        try {
            ePerson = accountService.getEPerson(context, registrationData.getToken());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

        if (ePerson != null) {
            registrationRest.setUser(ePerson.getID());
            try {
                MetadataRest<RegistrationMetadataRest> metadataRest = getMetadataRest(ePerson, registrationData);
                if (registrationData.getEmail() != null) {
                    metadataRest.put(
                        "email",
                        new RegistrationMetadataRest(registrationData.getEmail(), ePerson.getEmail())
                    );
                }
                registrationRest.setRegistrationMetadata(metadataRest);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            registrationRest.setRegistrationMetadata(getMetadataRest(registrationData));
        }

        return registrationRest;
    }


    private MetadataRest<RegistrationMetadataRest> getMetadataRest(EPerson ePerson, RegistrationData registrationData)
        throws SQLException {
        return registrationDataService.groupEpersonMetadataByRegistrationData(ePerson, registrationData)
                                      .reduce(
                                          new MetadataRest<>(),
                                          (map, entry) -> map.put(
                                              entry.getKey().getMetadataField().toString('.'),
                                              new RegistrationMetadataRest(
                                                  entry.getKey().getValue(),
                                                  entry.getValue().map(MetadataValue::getValue).orElse(null)
                                              )
                                          ),
                                          (m1, m2) -> {
                                              m1.getMap().putAll(m2.getMap());
                                              return m1;
                                          }
                                      );
    }

    private MetadataRest<RegistrationMetadataRest> getMetadataRest(RegistrationData registrationData) {
        MetadataRest<RegistrationMetadataRest> metadataRest = new MetadataRest<>();
        registrationData.getMetadata().forEach(
            (m) -> metadataRest.put(
                m.getMetadataField().toString('.'),
                new RegistrationMetadataRest(m.getValue())
            )
        );
        return metadataRest;
    }

    @Override
    public Class<RegistrationData> getModelClass() {
        return RegistrationData.class;
    }

}
