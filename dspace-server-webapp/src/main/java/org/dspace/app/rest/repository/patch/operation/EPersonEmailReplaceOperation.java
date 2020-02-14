/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /email", "value": "new@email"]'
 * </code>
 */
@Component
public class EPersonEmailReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_EMAIL = "/email";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            EPerson eperson = (EPerson) object;
            checkModelForExistingValue(eperson);
            eperson.setEmail((String) operation.getValue());
            return object;
        } else {
            throw new DSpaceBadRequestException("EPersonEmailReplaceOperation does not support this operation");
        }
    }

    /**
     * Checks whether the email of Eperson has an existing value to replace
     * @param ePerson   Object on which patch is being done
     */
    private void checkModelForExistingValue(EPerson ePerson) {
        if (ePerson.getEmail() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (e-mail).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_EMAIL));
    }
}
