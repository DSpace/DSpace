/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

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
public class EPersonPasswordReplaceOperation extends ReplacePatchOperation<EPersonRest, String> {

    @Override
    EPersonRest replace(EPersonRest eperson, Operation operation) {

        eperson.setPassword((String) operation.getValue());
        return eperson;
    }

    @Override
    void checkModelForExistingValue(EPersonRest resource, Operation operation) {
        /*
         * FIXME: the password field in eperson rest model is always null because
         * the value is not set in the rest converter.
         * We would normally throw an exception here since replace
         * operations are not allowed on non-existent values, but that
         * would prevent the password update from ever taking place.
         */
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
