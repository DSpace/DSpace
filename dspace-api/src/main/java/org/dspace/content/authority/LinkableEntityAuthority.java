/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

/**
 * Plugin interface that supplies mechanism for linkable entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface LinkableEntityAuthority extends ChoiceAuthority {

    /**
     * Get the linked entity type managed by the authority
     *
     * @return the linked entity types as an array of String
     */
    public String[] getLinkedEntityTypes();

    /**
     * Get the primary linked entity type managed by the authority
     *
     * @return the primary linked entity types as a String
     */
    public String getPrimaryLinkedEntityType();

    /**
     * Get the eternal source configured for this authority by given metadata key
     *
     * @return the linked external source identifier as a String
     */
    public Map<String, String> getExternalSource();

}
