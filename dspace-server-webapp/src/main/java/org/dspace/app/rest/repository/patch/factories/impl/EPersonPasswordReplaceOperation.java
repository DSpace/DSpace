/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /password", "value": "newpassword"]'
 * </code>
 */
@Component
public class EPersonPasswordReplaceOperation extends PatchOperation<EPerson> {

    /**
     * Path in json body of patch that uses this operation
     */
    public static final String OPERATION_PASSWORD_CHANGE = "/password";
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public EPerson perform(Context context, EPerson eperson, Operation operation) {
        checkOperationValue(operation.getValue());
        checkModelForExistingValue(eperson);
        ePersonService.setPassword(eperson, (String) operation.getValue());
        return eperson;
    }

    void checkModelForExistingValue(EPerson resource) {
        /*
         * FIXME: the password field in eperson rest model is always null because
         * the value is not set in the rest converter.
         * We would normally throw an exception here since replace
         * operations are not allowed on non-existent values, but that
         * would prevent the password update from ever taking place.
         */
    }

    @Override
    public boolean supports(DSpaceObject R, String path) {
        return (R instanceof EPerson && path.trim().equalsIgnoreCase(OPERATION_PASSWORD_CHANGE));
    }
}
