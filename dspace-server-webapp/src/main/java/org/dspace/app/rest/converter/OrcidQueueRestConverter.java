/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
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

    @Autowired
    private ItemService ItemService;

    @Override
    public OrcidQueueRest convert(OrcidQueue orcidQueue, Projection projection) {
        OrcidQueueRest rest = new OrcidQueueRest();

        Item entity = orcidQueue.getEntity();

        rest.setEntityId(entity != null ? entity.getID() : null);
        rest.setDescription(getDescription(orcidQueue, entity));
        rest.setRecordType(getRecordType(orcidQueue, entity));
        rest.setId(orcidQueue.getId());
        rest.setOwnerId(orcidQueue.getOwner().getID());
        rest.setPutCode(orcidQueue.getPutCode());
        rest.setProjection(projection);

        return rest;
    }

    private String getDescription(OrcidQueue orcidQueue, Item entity) {
        if (orcidQueue.getDescription() != null) {
            return orcidQueue.getDescription();
        }

        return entity != null ? getMetadataValue(entity, "dc.title") : null;

    }

    private String getRecordType(OrcidQueue orcidQueue, Item entity) {
        if (orcidQueue.getRecordType() != null) {
            return orcidQueue.getRecordType();
        } else {
            return ItemService.getEntityType(entity);
        }
    }

    private String getMetadataValue(Item item, String metadatafield) {
        return ItemService.getMetadataFirstValue(item, new MetadataFieldName(metadatafield), Item.ANY);
    }

    @Override
    public Class<OrcidQueue> getModelClass() {
        return OrcidQueue.class;
    }

}
