/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;
import org.springframework.stereotype.Component;

/**
 * This is the converter from a {@link org.dspace.content.authority.DSpaceControlledVocabularyIndex} to a
 * {@link org.dspace.app.rest.model.BrowseIndexRest#BROWSE_TYPE_HIERARCHICAL} {@link org.dspace.app.rest.model.BrowseIndexRest}
 *
 * @author Marie Verdonck (Atmire) on 04/05/2023
 */
@Component
public class HierarchicalBrowseConverter implements DSpaceConverter<DSpaceControlledVocabularyIndex, BrowseIndexRest> {

    @Override
    public BrowseIndexRest convert(DSpaceControlledVocabularyIndex obj, Projection projection) {
        BrowseIndexRest bir = new BrowseIndexRest();
        bir.setProjection(projection);
        bir.setId(obj.getVocabulary().getPluginInstanceName());
        bir.setBrowseType(BrowseIndexRest.BROWSE_TYPE_HIERARCHICAL);
        bir.setFacetType(obj.getFacetConfig().getIndexFieldName());
        bir.setVocabulary(obj.getVocabulary().getPluginInstanceName());
        bir.setMetadataList(new ArrayList<>(obj.getMetadataFields()));
        return bir;
    }

    @Override
    public Class<DSpaceControlledVocabularyIndex> getModelClass() {
        return DSpaceControlledVocabularyIndex.class;
    }
}
