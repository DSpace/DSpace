/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.util.UUID;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;

/**
 * Service that disciplines security around a {@link org.dspace.content.InProgressSubmission}
 * This service is asynchronous, thus an in progress item must be tracked first in the list of
 * workspace items to whom accessibility via submission definition is to be checked.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface InProgressSubmissionService {

    /**
     * add an {@link org.dspace.content.InProgressSubmission} object to the list of WorkspaceItems for
     * whom metadataAccessibility will be checked.
     *
     * Added entries, last in contained list for 10 minutes.
     *
     * @param inProgressSubmission
     */
    void add(InProgressSubmission inProgressSubmission);

    /**
     * Checks if for a given itemId belonging to a {@link org.dspace.content.InProgressSubmission} object
     * and {@link MetadataValue} value pair, this metadataValue has to be displayed as part of the
     * submission definition
     *
     * @param itemId
     * @param metadataValue
     * @return
     */
    boolean hasSubmissionRights(UUID itemId, final MetadataValue metadataValue);

    /**
     * Removes {@link org.dspace.content.InProgressSubmission} object with given id, if present, from internal list
     *
     * @param itemId
     */
    void remove(UUID itemId);
}
