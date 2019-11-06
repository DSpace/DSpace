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
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson netid patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /netid", "value": "newNetId"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonNetidReplaceOperation extends PatchOperation<EPersonRest> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NETID = "/netid";

    @Override
    public EPersonRest perform(EPersonRest eperson, Operation operation) {
        checkOperationValue(operation.getValue());
        checkModelForExistingValue(eperson);
        eperson.setNetid((String) operation.getValue());
        return eperson;
    }

    void checkModelForExistingValue(EPersonRest resource) {
        if (resource.getNetid() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    public boolean supports(RestModel R, String path) {
        return (R instanceof EPersonRest && path.trim().equalsIgnoreCase(OPERATION_PATH_NETID));
    }
}
