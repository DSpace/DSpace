/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;

/**
 * The interface for repository patch operations.
 *
 * @author Michael Spalti
 */
public interface ResourcePatchOperation<R extends RestModel> {

    /**
     * Updates the rest model by applying the patch operation.
     * @param resource the rest model
     * @param operation
     * @return the updated rest model
     * @throws PatchBadRequestException
     */
    RestModel perform(R resource, Operation operation)
            throws PatchBadRequestException;

}
