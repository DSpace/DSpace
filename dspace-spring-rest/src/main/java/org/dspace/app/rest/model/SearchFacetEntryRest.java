package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.DiscoveryRestController;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
public class SearchFacetEntryRest implements RestModel {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String name;

    @JsonIgnore
    private List<SearchFacetValueRest> values;

    public SearchFacetEntryRest(final String name) {
        this.name = StringUtils.substringBefore(name, ".year");
    }

    public String getCategory() {
        return CATEGORY;
    }

    @JsonIgnore
    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void addValue(final SearchFacetValueRest valueRest) {
        if(values == null) {
            values = new LinkedList<>();
        }

        values.add(valueRest);
    }

    public List<SearchFacetValueRest> getValues() {
        return values;
    }
}
