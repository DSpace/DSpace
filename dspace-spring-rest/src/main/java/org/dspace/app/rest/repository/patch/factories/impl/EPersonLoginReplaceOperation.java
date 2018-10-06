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
 * Implementation for EPerson canLogin patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /canLogin", "value": "true|false"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonLoginReplaceOperation extends PatchOperation<EPersonRest, String>
        implements ResourcePatchOperation<EPersonRest> {


    /**
     * Updates the canLogIn status in the eperson rest model.
     * @param resource the rest model
     * @param operation
     * @return the updated rest model
     * @throws PatchBadRequestException
     */
    @Override
    public EPersonRest perform(EPersonRest resource, Operation operation)
            throws PatchBadRequestException {

        return replace(resource, operation);
    }

    private EPersonRest replace(EPersonRest eperson, Operation operation)
            throws PatchBadRequestException {

        checkOperationValue(operation.getValue());
        Boolean canLogin = getBooleanOperationValue(operation.getValue());
        eperson.setCanLogIn(canLogin);
        return eperson;

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
