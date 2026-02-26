/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class IndexableObjectHighlightResultConverter implements DSpaceConverter<DiscoverResult.IndexableObjectHighlightResult, SearchResultEntryRest> {
    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    protected ConverterService converter;

    @Override
    public SearchResultEntryRest convert(DiscoverResult.IndexableObjectHighlightResult modelObject,
                                         Projection projection) {
        SearchResultEntryRest resultEntry = new SearchResultEntryRest();
        resultEntry.setProjection(projection);

        //Convert the DSpace Object to its REST model
        resultEntry.setIndexableObject(convertDSpaceObject(modelObject.getIndexableObject(), projection));

        if (MapUtils.isNotEmpty(modelObject.getHighlightResults())) {
            for (Map.Entry<String, List<String>> metadataHighlight : modelObject.getHighlightResults().entrySet()) {
                resultEntry.addHitHighlights(metadataHighlight.getKey(), metadataHighlight.getValue());
            }
        }

        return resultEntry;
    }

    @Override
    public Class<DiscoverResult.IndexableObjectHighlightResult> getModelClass() {
        return DiscoverResult.IndexableObjectHighlightResult.class;
    }

    private RestAddressableModel convertDSpaceObject(final IndexableObject indexableObject,
                                                     final Projection projection) {
        return converter.toRest(indexableObject.getIndexedObject(), projection);
    }
}
