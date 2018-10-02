/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.sql.SQLException;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.EPersonOperationFactory;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides PATCH operation implementations for EPerson updates.
 */
@Component
public class EPersonPatch extends AbstractResourcePatch<EPerson> {

    @Autowired
    EPersonOperationFactory patchFactory;

    /**
     * Performs the replace operation.
     * @param eperson dspace object
     * @param context dspace context
     * @param operation the replace operation
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void replace(EPerson eperson, Context context, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {

        // Get the patch operation via the EPerson replace operation factory.
        ResourcePatchOperation<EPerson> patchOperation = patchFactory.getReplaceOperationForPath(operation.getPath());
        patchOperation.perform(context, eperson, operation);

    }
}
