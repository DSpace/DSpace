/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The Browse Index REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(
            name = BrowseIndexRest.LINK_ITEMS,
            method = "listBrowseItems"
    ),
    @LinkRest(
            name = BrowseIndexRest.LINK_ENTRIES,
            method = "listBrowseEntries"
    )
})
public class BrowseIndexRest extends BaseObjectRest<String> {
    private static final long serialVersionUID = -4870333170249999559L;

    public static final String NAME = "browse";

    public static final String CATEGORY = RestAddressableModel.DISCOVER;

    public static final String LINK_ITEMS = "items";
    public static final String LINK_ENTRIES = "entries";
    public static final String LINK_VOCABULARY = "vocabulary";

    // if the browse index has two levels, the 1st level shows the list of entries like author names, subjects, types,
    // etc. the second level is the actual list of items linked to a specific entry
    public static final String BROWSE_TYPE_VALUE_LIST = "valueList";
    // if the browse index has one level: the full list of items
    public static final String BROWSE_TYPE_FLAT = "flatBrowse";
    // if the browse index should display the vocabulary tree. The 1st level shows the tree.
    // The second level is the actual list of items linked to a specific entry
    public static final String BROWSE_TYPE_HIERARCHICAL = "hierarchicalBrowse";

    // Shared fields
    String browseType;
    @JsonProperty(value = "metadata")
    List<String> metadataList;

    // Single browse index fields
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String dataType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<SortOption> sortOptions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String order;

    // Hierarchical browse fields
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String facetType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String vocabulary;

    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public List<String> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<String> metadataList) {
        this.metadataList = metadataList;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public List<SortOption> getSortOptions() {
        return sortOptions;
    }

    public void setSortOptions(List<SortOption> sortOptions) {
        this.sortOptions = sortOptions;
    }

    /**
     * - valueList => if the browse index has two levels, the 1st level shows the list of entries like author names,
     *      subjects, types, etc. the second level is the actual list of items linked to a specific entry
     * - flatBrowse if the browse index has one level: the full list of items
     * - hierarchicalBrowse if the browse index should display the vocabulary tree. The 1st level shows the tree.
     *      The second level is the actual list of items linked to a specific entry
     */
    public void setBrowseType(String browseType) {
        this.browseType = browseType;
    }

    public String getBrowseType() {
        return browseType;
    }

    public void setFacetType(String facetType) {
        this.facetType = facetType;
    }

    public String getFacetType() {
        return facetType;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }


    public String getVocabulary() {
        return vocabulary;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    static public class SortOption {
        private String name;
        private String metadata;

        public SortOption(String name, String metadata) {
            this.name = name;
            this.metadata = metadata;
        }

        public String getName() {
            return name;
        }

        public String getMetadata() {
            return metadata;
        }
    }
}
