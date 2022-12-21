/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RatingReviewActionAdvancedInfoRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.state.actions.processingaction.RatingReviewActionAdvancedInfo;

public class RatingReviewActionAdvancedInfoConverter
    implements DSpaceConverter<RatingReviewActionAdvancedInfo, RatingReviewActionAdvancedInfoRest> {

    @Override
    public RatingReviewActionAdvancedInfoRest convert(RatingReviewActionAdvancedInfo modelObject,
                                                      Projection projection) {
        RatingReviewActionAdvancedInfoRest restModel = new RatingReviewActionAdvancedInfoRest();
        restModel.setProjection(projection);
        restModel.setDescriptionRequired(modelObject.isDescriptionRequired());
        restModel.setMaxValue(modelObject.getMaxValue());
        restModel.setType(modelObject.getType());
        restModel.setId(modelObject.getId());
        return restModel;
    }

    @Override
    public Class<RatingReviewActionAdvancedInfo> getModelClass() {
        return RatingReviewActionAdvancedInfo.class;
    }
}
