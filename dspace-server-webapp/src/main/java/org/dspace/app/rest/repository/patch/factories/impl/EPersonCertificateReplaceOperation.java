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
 * Implementation for EPerson requires certificate patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /certificate", "value": true|false]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonCertificateReplaceOperation extends PatchOperation<EPersonRest> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_CERTIFICATE = "/certificate";

    @Override
    public EPersonRest perform(EPersonRest eperson, Operation operation) {
        checkOperationValue(operation.getValue());
        checkModelForExistingValue(eperson);
        Boolean requireCert = getBooleanOperationValue(operation.getValue());
        eperson.setRequireCertificate(requireCert);
        return eperson;

    }

    void checkModelForExistingValue(EPersonRest resource) {
        // TODO: many (all?) boolean values on the rest model should never be null.
        // So perhaps the error to throw in this case is different...IllegalStateException?
        // Or perhaps do nothing (no check is required).
        if ((Object) resource.isRequireCertificate() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    public boolean supports(RestModel R, String path) {
        return (R instanceof EPersonRest && path.trim().equalsIgnoreCase(OPERATION_PATH_CERTIFICATE));
    }
}
