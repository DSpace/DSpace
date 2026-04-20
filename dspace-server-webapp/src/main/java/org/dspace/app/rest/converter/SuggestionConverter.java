/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SuggestionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert a Suggestion to its REST representation, the
 * SuggestionRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SuggestionConverter
        implements DSpaceConverter<Suggestion, SuggestionRest> {

    @Autowired
    private MetadataValueDTOListConverter metadataConverter;

    @Override
    public SuggestionRest convert(Suggestion target, Projection projection) {
        SuggestionRest targetRest = new SuggestionRest();
        targetRest.setProjection(projection);
        targetRest.setId(target.getID());
        targetRest.setDisplay(target.getDisplay());
        targetRest.setExternalSourceUri(target.getExternalSourceUri());
        targetRest.setSource(target.getSource());
        targetRest.setScore(String.format("%.2f", target.getScore()));
        for (SuggestionEvidence se : target.getEvidences()) {
            targetRest.getEvidences().put(se.getName(),
                    new SuggestionRest.EvidenceRest(String.format("%.2f", se.getScore()), se.getNotes()));
        }
        targetRest.setMetadata(metadataConverter.convert(target.getMetadata()));
        return targetRest;
    }

    @Override
    public Class<Suggestion> getModelClass() {
        return Suggestion.class;
    }

}
