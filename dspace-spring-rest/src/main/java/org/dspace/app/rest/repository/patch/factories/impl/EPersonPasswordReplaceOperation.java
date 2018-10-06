/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /password", "value": "newpassword"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonPasswordReplaceOperation extends PatchOperation<EPersonRest, String>
        implements ResourcePatchOperation<EPersonRest> {


    /**
     * Updates the password in the eperson rest model.
     * @param resource the rest model
     * @param operation
     * @return updated rest model
     * @throws PatchBadRequestException
     */
    @Override
    public EPersonRest perform( EPersonRest resource, Operation operation)
            throws PatchBadRequestException {

        checkOperationValue(operation.getValue());
        resource.setPassword((String) operation.getValue());
        return resource;

    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }
}
