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
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /email", "value": "new@email"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonEmailReplaceOperation extends ReplacePatchOperation<EPersonRest, String>
        implements ResourcePatchOperation<EPersonRest> {
    @Override
    EPersonRest replace(EPersonRest eperson, Operation operation) {

        eperson.setEmail((String) operation.getValue());
        return eperson;
    }

    @Override
    void checkModelForExistingValue(EPersonRest resource, Operation operation) {
        if (resource.getEmail() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
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
