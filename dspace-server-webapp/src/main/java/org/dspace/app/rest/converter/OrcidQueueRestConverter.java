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
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the OrcidQueue in the DSpace API data model and
 * the REST data model.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class OrcidQueueRestConverter implements DSpaceConverter<OrcidQueue, OrcidQueueRest> {

    @Override
    public OrcidQueueRest convert(OrcidQueue modelObject, Projection projection) {
        OrcidQueueRest rest = new OrcidQueueRest();
        rest.setEntityId(modelObject.getEntity().getID());
        rest.setEntityName(getMetadataValueFromItem(modelObject.getEntity(), "dc.title"));
        rest.setEntityType(getMetadataValueFromItem(modelObject.getEntity(), "relationship.type"));
        rest.setId(modelObject.getId());
        rest.setOwnerId(modelObject.getOwner().getID());
        rest.setProjection(projection);
        return rest;
    }

    private String getMetadataValueFromItem(Item entity, String metadataField) {
        return entity.getMetadata().stream()
            .filter(metadata -> metadata.getMetadataField().toString('.').equals(metadataField))
            .map(metadata -> metadata.getValue())
            .findFirst()
            .orElse(null);
    }

    @Override
    public Class<OrcidQueue> getModelClass() {
        return OrcidQueue.class;
    }

}
