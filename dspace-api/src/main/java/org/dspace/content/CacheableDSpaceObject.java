/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import jakarta.persistence.Cacheable;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Abstract class for DSpaceObjects which are safe to cache in Hibernate's second level cache.
 * See hibernate-ehcache-config.xml for caching configurations for each DSpaceObject which extends this class.
 */
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public abstract class CacheableDSpaceObject extends DSpaceObject {
}
