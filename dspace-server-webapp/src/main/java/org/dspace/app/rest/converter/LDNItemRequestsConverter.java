/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.ldn.model.ItemRequests;
import org.dspace.app.rest.model.LDNItemRequestsRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the ItemRequests in the DSpace API data model and
 * the REST data model
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 */
@Component
public class LDNItemRequestsConverter  implements DSpaceConverter<ItemRequests, LDNItemRequestsRest> {

    @Override
    public LDNItemRequestsRest convert(ItemRequests modelObject, Projection projection) {
        LDNItemRequestsRest result = new LDNItemRequestsRest();
        result.setItemuuid(modelObject.getItemUuid());
        return result;
    }

    @Override
    public Class<ItemRequests> getModelClass() {
        return ItemRequests.class;
    }

}
