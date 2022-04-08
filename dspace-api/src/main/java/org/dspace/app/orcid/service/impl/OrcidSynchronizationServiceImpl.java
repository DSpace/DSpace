/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.SQLException;
import java.util.stream.Stream;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidSynchronizationService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidSynchronizationServiceImpl implements OrcidSynchronizationService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public void linkProfile(Context context, Item profile, OrcidTokenResponseDTO token) throws SQLException {

        String orcid = token.getOrcid();
        String accessToken = token.getAccessToken();
        String refreshToken = token.getRefreshToken();
        String[] scopes = token.getScopeAsArray();

        itemService.setMetadataSingleValue(context, profile, "person", "identifier", "orcid", null, orcid);
        itemService.setMetadataSingleValue(context, profile, "dspace", "orcid", "access-token", null, accessToken);
        itemService.setMetadataSingleValue(context, profile, "dspace", "orcid", "refresh-token", null, refreshToken);
        itemService.clearMetadata(context, profile, "dspace", "orcid", "scope", Item.ANY);
        for (String scope : scopes) {
            itemService.addMetadata(context, profile, "dspace", "orcid", "scope", null, scope);
        }

        if (isBlank(itemService.getMetadataFirstValue(profile, "dspace", "orcid", "authenticated", Item.ANY))) {
            String currentDate = ISO_DATE_TIME.format(now());
            itemService.setMetadataSingleValue(context, profile, "dspace", "orcid", "authenticated", null, currentDate);
        }

        EPerson ePerson = ePersonService.findByProfileItem(context, profile);
        EPerson ePersonByOrcid = ePersonService.findByNetid(context, orcid);
        if (ePerson != null && ePersonByOrcid == null && isBlank(ePerson.getNetid())) {
            ePerson.setNetid(orcid);
            updateEPerson(context, ePerson);
        }

        updateItem(context, profile);

    }

    private Stream<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return item.getMetadata().stream()
            .filter(metadata -> metadataField.equals(metadata.getMetadataField().toString('.')));
    }

    private void updateItem(Context context, Item item) throws SQLException {
        try {
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void updateEPerson(Context context, EPerson ePerson) throws SQLException {
        try {
            context.turnOffAuthorisationSystem();
            ePersonService.update(context, ePerson);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }
}
