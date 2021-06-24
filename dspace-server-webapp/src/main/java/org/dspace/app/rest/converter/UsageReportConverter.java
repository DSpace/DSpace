/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * Converter so list of UsageReportRest can be converted in to a rest page
 *
 * @author Maria Verdonck (Atmire) on 11/06/2020
 */
@Component
public class UsageReportConverter implements DSpaceConverter<UsageReportRest, UsageReportRest> {
    @Override
    public UsageReportRest convert(UsageReportRest modelObject, Projection projection) {
        modelObject.setProjection(projection);
        return modelObject;
    }

    @Override
    public Class<UsageReportRest> getModelClass() {
        return UsageReportRest.class;
    }
}
