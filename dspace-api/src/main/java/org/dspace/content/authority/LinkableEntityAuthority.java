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
     * @return the linked entity type as a String
     */
    String getLinkedEntityType();

    /**
     * Get the eternal source configured for this authority by given metadata key
     *
     * @return the linked external source identifier as a String
     */
    Map<String, String> getExternalSource();

}
