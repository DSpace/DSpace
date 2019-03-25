/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * This class is used as a representation of MetadataValues for the MetadataValues that are derived from the
 * Relationships that the item has. This includes the useForPlace property which we'll have to use to determine
 * whether these Values should be counted for place calculation on both the native MetadataValues and the
 * Relationship's place attributes.
 */
public class RelationshipMetadataValue extends MetadataValue {

    /**
     * This property determines whether this RelationshipMetadataValue should be used in place calculation or not
     */
    private boolean useForPlace;

    /**
     * This property determines whether this RelationshipMetadataValue should be used in place calculation or not.
     * This is retrieved from Spring configuration when constructing RelationshipMetadataValues. This Spring
     * configuration is located in the core-services.xml configuration file.
     * Putting this property on true will imply that we're now mixing plain-text metadatavalues with the
     * metadatavalues that are constructed through Relationships with regards to the place attribute.
     * For example, currently the RelationshipMetadataValue dc.contributor.author that is constructed through a
     * Relationship for a Publication will have its useForPlace set to true. This means that the place
     * calculation will take both these RelationshipMetadataValues into account together with the normal
     * plain text metadatavalues.
     */
    public boolean isUseForPlace() {
        return useForPlace;
    }

    public void setUseForPlace(boolean useForPlace) {
        this.useForPlace = useForPlace;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != RelationshipMetadataValue.class) {
            return false;
        }
        final RelationshipMetadataValue other = (RelationshipMetadataValue) obj;
        if (this.isUseForPlace() != other.isUseForPlace()) {
            return false;
        }
        return super.equals(obj);
    }
}
