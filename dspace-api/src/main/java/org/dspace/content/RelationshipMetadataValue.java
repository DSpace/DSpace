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
