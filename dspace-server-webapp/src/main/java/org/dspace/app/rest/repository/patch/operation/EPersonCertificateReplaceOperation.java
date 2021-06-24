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
 * Implementation for EPerson requires certificate patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /certificate", "value": true|false]'
 * </code>
 */
@Component
public class EPersonCertificateReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_CERTIFICATE = "/certificate";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        Boolean requireCert = getBooleanOperationValue(operation.getValue());
        if (supports(object, operation)) {
            EPerson eperson = (EPerson) object;
            eperson.setRequireCertificate(requireCert);
            return object;
        } else {
            throw new DSpaceBadRequestException("EPersonCertificateReplaceOperation does not support this operation.");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof EPerson && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_CERTIFICATE));
    }
}
