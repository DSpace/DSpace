/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Base class for all resource patch operations.
 */
public abstract class PatchOperation<M extends DSpaceObject>
        implements ResourcePatchOperation<M> {

    /**
     * Updates the rest model by applying the patch operation.
     *
     * @param context   context of patch operation
     * @param resource  the dso.
     * @param operation the patch operation.
     * @return the patched dso
     * @throws DSpaceBadRequestException
     */
    public abstract M perform(Context context, M resource, Operation operation);

    /**
     * Throws PatchBadRequestException for missing operation value.
     *
     * @param value the value to test
     */
    void checkOperationValue(Object value) {
        if (value == null) {
            throw new DSpaceBadRequestException("No value provided for the operation.");
        }
    }

    /**
     * Allows clients to use either a boolean or a string representation of boolean value.
     *
     * @param value the operation value
     * @return the original or derived boolean value
     * @throws DSpaceBadRequestException
     */
    Boolean getBooleanOperationValue(Object value) {
        Boolean bool;

        if (value instanceof String) {
            bool = BooleanUtils.toBooleanObject((String) value);
            if (bool == null) {
                // make sure the string was converted to boolean.
                throw new DSpaceBadRequestException("Boolean value not provided.");
            }
        } else {
            bool = (Boolean) value;
        }
        return bool;
    }

    /**
     * Determines whether or not this Patch Operation can do this patch (RestModel and path gets checked)
     * @param M         dso, whose class must be instance of dso for which this PatchOperation was created
     * @param path      Path given to the patch body, should match this type of Patch Operation
     * @return          True if this PatchOperation class can do the patch for this given dso type and Path
     */
    public abstract boolean supports(DSpaceObject M, String path);

}
