/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Utility methods for working with Hibernate proxies.
 * This class existed in Hibernate 5 but was removed from v6.
 * https://github.com/hibernate/hibernate-orm/blob/5.6/hibernate-core/src/main/java/org/hibernate/proxy/HibernateProxyHelper.java
 * We've copied it into DSpace to utilize the below utility method.
 */
public final class HibernateProxyHelper {

    /**
     * Get the class of an instance or the underlying class
     * of a proxy (without initializing the proxy!). It is
     * almost always better to use the entity name!
     */
    public static Class getClassWithoutInitializingProxy(Object object) {
        if (object instanceof HibernateProxy) {
            HibernateProxy proxy = (HibernateProxy) object;
            LazyInitializer li = proxy.getHibernateLazyInitializer();
            return li.getPersistentClass();
        } else {
            return object.getClass();
        }
    }

    private HibernateProxyHelper() {
        //can't instantiate
    }
}
