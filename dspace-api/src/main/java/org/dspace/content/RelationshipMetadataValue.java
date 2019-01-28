/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.hibernate.proxy.HibernateProxyHelper;

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
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass) {
            return false;
        }
        final RelationshipMetadataValue other = (RelationshipMetadataValue) obj;
        if (this.isUseForPlace() != other.isUseForPlace()) {
            return false;
        }
        return super.equals(obj);
    }
}
