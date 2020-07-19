/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * Plugin interface that supplies mechanism for linkable entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface LinkableEntityAuthority {

    /**
     * Get the linked entity type managed by the authority
     *
     * @return the linked entity type as a String
     */
    public String getLinkedEntityType();

}
