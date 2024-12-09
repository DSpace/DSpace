/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.List;

import org.dspace.authority.AuthorityValue;

/**
 * This interface describes beans to be used for the {@link AuthorityVirtualMetadataPopulator} implementation.
 * It achieves virtual metadata that looks and acts identical to the entity-driven {@link VirtualMetadataConfiguration}
 * but uses Authority values as the source for metadata values instead of a related DSpace Item
 *
 * Functionality like 'useNameVariants' and 'useForPlace' are not implemented as authority values typically don't
 * have enough information to derive this.
 *
 * @author Kim Shepherd
 */
public interface AuthorityVirtualMetadataConfiguration {

    /**
     * Retrieve the values from the given AuthorityValue "otherMetadataMap" for each configured fields property
     * and return as a list. The authority value is responsible for how to store and return this map.
     *
     * @param authorityValue The authority value from which to build the list of values
     * @return The String values for each field configured
     */
    List<String> getValues(AuthorityValue authorityValue);
}
