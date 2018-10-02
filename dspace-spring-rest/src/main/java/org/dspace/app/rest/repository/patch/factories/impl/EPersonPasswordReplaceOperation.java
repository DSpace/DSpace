/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class EPersonPasswordReplaceOperation extends PatchOperation<EPerson, String>
        implements ResourcePatchOperation<EPerson> {

    @Autowired
    EPersonService epersonService;

    @Override
    public void perform(Context context, EPerson resource, Operation operation)
            throws PatchBadRequestException {

        replace(context, resource, operation);
    }

    private void replace(Context context, EPerson ePerson, Operation operation) throws PatchBadRequestException {

        checkOperationValue((String) operation.getValue());

        epersonService.setPassword(ePerson, (String) operation.getValue());

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
