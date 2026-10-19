/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_FLAT;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_HIERARCHICAL;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_VALUE_LIST;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the BrowseIndex in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class BrowseIndexConverter implements DSpaceConverter<BrowseIndex, BrowseIndexRest> {

    @Override
    public BrowseIndexRest convert(BrowseIndex obj, Projection projection) {
        BrowseIndexRest bir = new BrowseIndexRest();
        bir.setProjection(projection);
        List<String> metadataList = new ArrayList<String>();
        String id = obj.getName();
        if (obj instanceof DSpaceControlledVocabularyIndex) {
            DSpaceControlledVocabularyIndex vocObj = (DSpaceControlledVocabularyIndex) obj;
            metadataList = new ArrayList<>(vocObj.getMetadataFields());
            id = vocObj.getVocabulary().getPluginInstanceName();
            bir.setFacetType(vocObj.getFacetConfig().getIndexFieldName());
            bir.setVocabulary(vocObj.getVocabulary().getPluginInstanceName());
            bir.setBrowseType(BROWSE_TYPE_HIERARCHICAL);
        } else if (obj.isMetadataIndex()) {
            for (String s : obj.getMetadata().split(",")) {
                metadataList.add(s.trim());
            }
            bir.setDataType(obj.getDataType());
            bir.setOrder(obj.getDefaultOrder());
            bir.setBrowseType(BROWSE_TYPE_VALUE_LIST);
        } else {
            metadataList.add(obj.getSortOption().getMetadata());
            bir.setDataType(obj.getDataType());
            bir.setOrder(obj.getDefaultOrder());
            bir.setBrowseType(BROWSE_TYPE_FLAT);
        }
        bir.setId(id);
        bir.setMetadataList(metadataList);

        List<BrowseIndexRest.SortOption> sortOptionsList = new ArrayList<BrowseIndexRest.SortOption>();
        try {
            for (SortOption so : SortOption.getSortOptions()) {
                sortOptionsList.add(new BrowseIndexRest.SortOption(so.getName(), so.getMetadata()));
            }
        } catch (SortException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (!bir.getBrowseType().equals(BROWSE_TYPE_HIERARCHICAL)) {
            bir.setSortOptions(sortOptionsList);
        }
        return bir;
    }

    @Override
    public Class<BrowseIndex> getModelClass() {
        return BrowseIndex.class;
    }
}
