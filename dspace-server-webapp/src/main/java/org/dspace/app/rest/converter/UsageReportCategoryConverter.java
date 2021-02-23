/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.UsageReportCategoryRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * Converter so list of UsageReportCategoryRest can be converted in to a rest page
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class UsageReportCategoryConverter implements DSpaceConverter<UsageReportCategoryRest, UsageReportCategoryRest> {
    @Override
    public UsageReportCategoryRest convert(UsageReportCategoryRest modelObject, Projection projection) {
        modelObject.setProjection(projection);
        return modelObject;
    }

    @Override
    public Class<UsageReportCategoryRest> getModelClass() {
        return UsageReportCategoryRest.class;
    }
}
