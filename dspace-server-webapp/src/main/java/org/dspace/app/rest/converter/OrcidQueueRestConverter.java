/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.orcid.OrcidQueue;
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
    public OrcidQueueRest convert(OrcidQueue orcidQueue, Projection projection) {
        OrcidQueueRest rest = new OrcidQueueRest();

        Item entity = orcidQueue.getEntity();

        rest.setEntityId(entity != null ? entity.getID() : null);
        rest.setDescription(orcidQueue.getDescription());
        rest.setRecordType(orcidQueue.getRecordType());
        rest.setId(orcidQueue.getID());
        rest.setProfileItemId(orcidQueue.getProfileItem().getID());
        rest.setOperation(orcidQueue.getOperation() != null ? orcidQueue.getOperation().name() : null);
        rest.setProjection(projection);

        return rest;
    }

    @Override
    public Class<OrcidQueue> getModelClass() {
        return OrcidQueue.class;
    }

}
