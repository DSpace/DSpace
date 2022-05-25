/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CommunityGroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.CommunityGroup;
import org.dspace.discovery.IndexableObject;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the laimTask in the DSpace API data model
 * and the REST data model
 *
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 */
@Component
public class CommunityGroupConverter
        implements IndexableObjectConverter<CommunityGroup, CommunityGroupRest> {

    @Override
    public CommunityGroupRest convert(CommunityGroup obj, Projection projection) {
        CommunityGroupRest cgRest = new CommunityGroupRest();
        cgRest.setProjection(projection);
        cgRest.setId(obj.getID());
        cgRest.setName(obj.getName());
        cgRest.setShortName(obj.getShortName());
        return cgRest;
    }

    @Override
    public Class<CommunityGroup> getModelClass() {
        return CommunityGroup.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof CommunityGroup;
    }
}
