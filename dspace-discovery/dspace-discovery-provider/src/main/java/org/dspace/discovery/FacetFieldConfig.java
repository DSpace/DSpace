package org.dspace.discovery;

/**
 * Class contains all the data that can be configured for a facet field
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class FacetFieldConfig {
    private String field;
    private boolean isDate;
    /* The facet prefix, all facet values will have to start with the given prefix */
    private String prefix;

    public FacetFieldConfig(String field, boolean date) {
        this.field = field;
        isDate = date;
    }

    public FacetFieldConfig(String field, boolean date, String prefix) {
        this.prefix = prefix;
        this.field = field;
        isDate = date;
    }

    public String getField() {
        return field;
    }

    public boolean isDate() {
        return isDate;
    }

    public String getPrefix() {
        return prefix;
    }
}
