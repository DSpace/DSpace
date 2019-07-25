/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson canLogin patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /canLogin", "value": true|false]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonLoginReplaceOperation extends ReplacePatchOperation<EPersonRest, Boolean>
        implements ResourcePatchOperation<EPersonRest> {

    @Override
    public EPersonRest replace(EPersonRest eperson, Operation operation) {

        Boolean canLogin = getBooleanOperationValue(operation.getValue());
        eperson.setCanLogIn(canLogin);
        return eperson;
    }

    @Override
    void checkModelForExistingValue(EPersonRest resource, Operation operation) {
        if ((Object) resource.isCanLogIn() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    protected Class<Boolean[]> getArrayClassForEvaluation() {
        return Boolean[].class;
    }

    @Override
    protected Class<Boolean> getClassForEvaluation() {
        return Boolean.class;
    }
}
