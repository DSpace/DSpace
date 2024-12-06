/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SuggestionTargetRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.suggestion.SuggestionTarget;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert a SuggestionTarget to its REST representation, the
 * SuggestionTargetRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SuggestionTargetConverter
        implements DSpaceConverter<SuggestionTarget, SuggestionTargetRest> {

    @Override
    public SuggestionTargetRest convert(SuggestionTarget target, Projection projection) {
        SuggestionTargetRest targetRest = new SuggestionTargetRest();
        targetRest.setProjection(projection);
        targetRest.setId(target.getID());
        if (target != null && target.getTarget() != null) {
            targetRest.setDisplay(target.getTarget().getName());
        }
        targetRest.setTotal(target.getTotal());
        targetRest.setSource(target.getSource());
        return targetRest;
    }

    @Override
    public Class<SuggestionTarget> getModelClass() {
        return SuggestionTarget.class;
    }

}
