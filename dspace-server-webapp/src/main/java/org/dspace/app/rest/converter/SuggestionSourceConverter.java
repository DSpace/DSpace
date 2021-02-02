/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SuggestionSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.suggestion.SuggestionSource;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert a SuggestionSource to its REST representation, the
 * SuggestionSourceRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SuggestionSourceConverter
        implements DSpaceConverter<SuggestionSource, SuggestionSourceRest> {

    @Override
    public SuggestionSourceRest convert(SuggestionSource target, Projection projection) {
        SuggestionSourceRest targetRest = new SuggestionSourceRest();
        targetRest.setProjection(projection);
        targetRest.setId(target.getID());
        targetRest.setTotal(target.getTotal());
        return targetRest;
    }

    @Override
    public Class<SuggestionSource> getModelClass() {
        return SuggestionSource.class;
    }

}
