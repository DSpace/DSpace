/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;

import org.apache.commons.lang.BooleanUtils;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class EPersonLoginReplaceOperation extends PatchOperation<EPerson, String>
        implements ResourcePatchOperation<EPerson> {

    @Autowired
    EPersonService epersonService;

    @Override
    public void perform(Context context, EPerson resource, Operation operation)
            throws PatchBadRequestException, SQLException, AuthorizeException {

        replace(context, resource, operation);
    }

    private void replace(Context context, EPerson eperson, Operation operation)
            throws PatchBadRequestException, SQLException, AuthorizeException {

        checkOperationValue((String) operation.getValue());
        Boolean canLogin = BooleanUtils.toBooleanObject((String) operation.getValue());

        if (canLogin == null) {
            // make sure string was converted to boolean.
            throw new PatchBadRequestException("Boolean value not provided for canLogin operation.");
        }
        eperson.setCanLogIn(canLogin);

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
