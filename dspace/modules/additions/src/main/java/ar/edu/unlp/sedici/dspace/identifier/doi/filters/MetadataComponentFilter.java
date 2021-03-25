package ar.edu.unlp.sedici.dspace.identifier.doi.filters;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Required;

public class MetadataComponentFilter {
    /**
     * Contains the metadata element name, following the form "schema"."element"."qualifier".
     */
    private String metadataName;

    /**
     * (OPTIONAL) Regular expression that determines if metadata has a valid content for this filter. Follows the 
     * Java regular expressions determined in java.util.regex.
     * 
     */
    private String valueRegexFilter;

    public String getMetadataName() {
        return metadataName;
    }

    @Required
    public void setMetadataName(String metadataName) {
        this.metadataName = metadataName;
    }

    public void setValueRegexFilter(String valueRegexFilter) {
        this.valueRegexFilter = valueRegexFilter;
    }

    /**
     * Used to test if a given value matches the regular expression for this metadata filter component. 
     * If no regex value was set, then this returns true.
     */
    public boolean matchRegex(String targetValue) {
        //TODO test if regular expression is valid...
        if(valueRegexFilter == null || valueRegexFilter.isEmpty()) {
            return true;
        } else {
            return Pattern.matches(valueRegexFilter, targetValue);
        }
    }
}
