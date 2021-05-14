/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.dspace.content.Item.ANY;

import java.sql.SQLException;

import org.dspace.app.profile.service.AfterResearcherProfileCreationAction;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link AfterResearcherProfileCreationAction} that, if the
 * given profile has an orcid id, enrich it from the data taken from the orcid
 * registry.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileEnricher implements AfterResearcherProfileCreationAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidProfileEnricher.class);

    @Autowired
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    @Autowired
    private ItemService itemService;

    @Override
    public void perform(Context context, ResearcherProfile researcherProfile, EPerson owner) throws SQLException {
        if (!researcherProfile.getOrcid().isPresent()) {
            return;
        }

        String orcid = researcherProfile.getOrcid().get();
        Item profile = researcherProfile.getItem();

        try {

            orcidV3AuthorDataProvider.getExternalDataObject(orcid)
                .ifPresent(externalDataObject -> enrichProfile(context, externalDataObject, profile));

        } catch (Exception ex) {
            LOGGER.error("An error occurs during the enrichment of the"
                + " profile by obtaining data from the orcid registry", ex);
        }
    }

    private void enrichProfile(Context context, ExternalDataObject externalDataObject, Item profile) {
        externalDataObject.getMetadata().stream()
            .filter(metadataValue -> notAlreadyPresent(profile, metadataValue))
            .forEach(metadataValue -> addMetadata(context, profile, metadataValue));
    }

    private boolean notAlreadyPresent(Item item, MetadataValueDTO value) {
        return isEmpty(itemService.getMetadata(item, value.getSchema(), value.getElement(), value.getQualifier(), ANY));
    }

    private void addMetadata(Context context, Item profile, MetadataValueDTO value) {
        try {
            itemService.addMetadata(context, profile, value.getSchema(), value.getElement(),
                value.getQualifier(), value.getLanguage(), value.getValue());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
