/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson requires certificate patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /certificate", "value": "true|false"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonCertificateReplaceOperation extends ReplacePatchOperation<EPersonRest, String>
        implements ResourcePatchOperation<EPersonRest> {

    @Override
    public EPersonRest perform(EPersonRest resource, Operation operation)
            throws PatchBadRequestException {

        return replace(resource, operation);
    }

    @Override
    public EPersonRest replace(EPersonRest eperson, Operation operation)
            throws PatchBadRequestException {

        checkOperationValue(operation.getValue());
        checkModelForExistingValue(eperson.isRequireCertificate());

        Boolean requireCert = getBooleanOperationValue(operation.getValue());
        eperson.setRequireCertificate(requireCert);
        return eperson;

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
