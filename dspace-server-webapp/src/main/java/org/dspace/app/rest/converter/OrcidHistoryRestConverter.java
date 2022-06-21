/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.OrcidHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.orcid.OrcidHistory;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the OrcidHistory in the DSpace API data model and
 * the REST data model.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class OrcidHistoryRestConverter implements DSpaceConverter<OrcidHistory, OrcidHistoryRest> {

    @Override
    public OrcidHistoryRest convert(OrcidHistory modelObject, Projection projection) {
        OrcidHistoryRest rest = new OrcidHistoryRest();
        rest.setId(modelObject.getID());
        rest.setProfileItemId(modelObject.getProfileItem().getID());
        rest.setEntityId(modelObject.getEntity() != null ? modelObject.getEntity().getID() : null);
        rest.setResponseMessage(modelObject.getResponseMessage());
        rest.setStatus(modelObject.getStatus());
        rest.setTimestamp(modelObject.getTimestamp());
        rest.setProjection(projection);
        rest.setPutCode(modelObject.getPutCode());
        return rest;
    }

    @Override
    public Class<OrcidHistory> getModelClass() {
        return OrcidHistory.class;
    }

}
