/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the OrcidQueue in the DSpace API data model and
 * the REST data model.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class OrcidQueueRestConverter implements DSpaceConverter<OrcidQueue, OrcidQueueRest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidQueueRestConverter.class);

    @Autowired
    private ItemService ItemService;

    @Autowired
    private OrcidClient orcidClient;

    @Override
    public OrcidQueueRest convert(OrcidQueue orcidQueue, Projection projection) {
        OrcidQueueRest rest = new OrcidQueueRest();

        Item entity = orcidQueue.getEntity();

        rest.setEntityId(entity != null ? entity.getID() : null);
        rest.setEntityName(getEntityName(orcidQueue, entity));
        rest.setEntityType(getEntityType(orcidQueue, entity));
        rest.setId(orcidQueue.getId());
        rest.setOwnerId(orcidQueue.getOwner().getID());
        rest.setPutCode(orcidQueue.getPutCode());
        rest.setProjection(projection);

        return rest;
    }

    private String getEntityName(OrcidQueue orcidQueue, Item entity) {
        if (entity != null) {
            return getMetadataValue(entity, "dc.title");
        }

        return getEntityNameOnOrcid(orcidQueue);

    }

    private String getEntityType(OrcidQueue orcidQueue, Item entity) {
        if (orcidQueue.getEntityType() != null) {
            return orcidQueue.getEntityType();
        } else {
            return ItemService.getEntityType(entity);
        }
    }

    private String getEntityNameOnOrcid(OrcidQueue orcidQueue) {

        String orcid = getMetadataValue(orcidQueue.getOwner(), "person.identifier.orcid");
        String token = getMetadataValue(orcidQueue.getOwner(), "cris.orcid.access-token");
        String putCode = orcidQueue.getPutCode();

        if (StringUtils.isAnyEmpty(orcid, token, putCode)) {
            LOGGER.warn("It is not possible to find the name of the entity with these parameters: "
                + "orcid '{}' - token '{}' - putCode '{}'", orcid, token, putCode);
            return null;
        }

        try {

            switch (orcidQueue.getEntityType()) {
                case "Publication":
                    return getWorkTitle(orcid, token, putCode);
                case "Project":
                    return getFundingTitle(orcid, token, putCode);
                default:
                    return null;
            }

        } catch (Exception ex) {
            LOGGER.error(format("An error occurs retriving entity with putCode %s for "
                + "the orcid %s", putCode, orcid), ex);
            return null;
        }

    }

    private String getFundingTitle(String token, String orcid, String putCode) {
        return orcidClient.getFunding(token, orcid, putCode)
            .map(Funding::getTitle)
            .map(FundingTitle::getTitle)
            .map(Title::getContent)
            .orElse(null);
    }

    private String getWorkTitle(String token, String orcid, String putCode) {
        return orcidClient.getWork(token, orcid, putCode)
            .map(Work::getWorkTitle)
            .map(WorkTitle::getTitle)
            .map(Title::getContent)
            .orElse(null);
    }

    private String getMetadataValue(Item item, String metadatafield) {
        return ItemService.getMetadataFirstValue(item, new MetadataFieldName(metadatafield), Item.ANY);
    }

    @Override
    public Class<OrcidQueue> getModelClass() {
        return OrcidQueue.class;
    }

}
