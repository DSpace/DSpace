/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.hibernate.proxy.HibernateProxyHelper;

public class RelationshipMetadataValue extends MetadataValue {

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
