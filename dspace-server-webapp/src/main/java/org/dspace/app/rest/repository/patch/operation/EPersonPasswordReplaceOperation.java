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
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /password", "value": "newpassword"]'
 * </code>
 */
@Component
public class EPersonPasswordReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    public static final String OPERATION_PASSWORD_CHANGE = "/password";
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation)) {
            EPerson eperson = (EPerson) object;
            checkModelForExistingValue(eperson);
            ePersonService.setPassword(eperson, (String) operation.getValue());
            return object;
        } else {
            throw new DSpaceBadRequestException("EPersonPasswordReplaceOperation does not support this operation");
        }
    }

    /**
     * Checks whether the ePerson has a password via the ePersonService to checking if it has a non null password hash
     *      throws a DSpaceBadRequestException if not pw hash was present
     * @param ePerson   Object on which patch is being performed
     */
    private void checkModelForExistingValue(EPerson ePerson) {
        if (ePersonService.getPasswordHash(ePerson) == null
                || ePersonService.getPasswordHash(ePerson).getHash() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value (netID).");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PASSWORD_CHANGE));
    }
}
