/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySortFieldConfiguration {

    private String metadataField;
    private String type = DiscoveryConfigurationParameters.TYPE_TEXT;

    /** Attributes used for sorting of results **/
    public enum SORT_ORDER {
        desc,
        asc
    }

    private SORT_ORDER defaultSortOrder;

    /** Ignores special chars in front of the title when indexing into Solr. */
    private boolean ignoreLeadingNonAlphaNum = false;

    /** Ignores digits in front of the title when indexing into Solr. */
    private boolean ignoreLeadingDigits = false;

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SORT_ORDER getDefaultSortOrder() {
        return defaultSortOrder;
    }

    @Autowired(required = true)
    public void setDefaultSortOrder(SORT_ORDER defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    public boolean isIgnoreLeadingNonAlphaNum() {
        return ignoreLeadingNonAlphaNum;
    }

    @Autowired(required = false)
    public void setIgnoreLeadingNonAlphaNum(boolean ignoreLeadingNonAlphaNum) {
        this.ignoreLeadingNonAlphaNum = ignoreLeadingNonAlphaNum;
    }

    public boolean isIgnoreLeadingDigits() {
        return ignoreLeadingDigits;
    }

    @Autowired(required = false)
    public void setIgnoreLeadingDigits(boolean ignoreLeadingDigits) {
        this.ignoreLeadingDigits = ignoreLeadingDigits;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DiscoverySortFieldConfiguration) {
            DiscoverySortFieldConfiguration compareConfig = (DiscoverySortFieldConfiguration) obj;
            if (compareConfig.getMetadataField().equals(getMetadataField()) && compareConfig.getType()
                                                                                            .equals(getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 19)
            .append(this.getMetadataField())
            .append(this.getType())
            .toHashCode();
    }
}
