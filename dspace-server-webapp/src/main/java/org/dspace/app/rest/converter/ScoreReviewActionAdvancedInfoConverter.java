/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ScoreReviewActionAdvancedInfoRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.state.actions.processingaction.ScoreReviewActionAdvancedInfo;

/**
 * This converter is responsible for transforming the model representation of a ScoreReviewActionAdvancedInfo to
 * the REST representation of a ScoreReviewActionAdvancedInfo
 */
public class ScoreReviewActionAdvancedInfoConverter
    implements DSpaceConverter<ScoreReviewActionAdvancedInfo, ScoreReviewActionAdvancedInfoRest> {

    @Override
    public ScoreReviewActionAdvancedInfoRest convert(ScoreReviewActionAdvancedInfo modelObject,
                                                      Projection projection) {
        ScoreReviewActionAdvancedInfoRest restModel = new ScoreReviewActionAdvancedInfoRest();
        restModel.setDescriptionRequired(modelObject.isDescriptionRequired());
        restModel.setMaxValue(modelObject.getMaxValue());
        restModel.setType(modelObject.getType());
        restModel.setId(modelObject.getId());
        return restModel;
    }

    @Override
    public Class<ScoreReviewActionAdvancedInfo> getModelClass() {
        return ScoreReviewActionAdvancedInfo.class;
    }
}
