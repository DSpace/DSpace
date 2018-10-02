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
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson requres certificate patches.
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
public class EPersonCertificateReplaceOperation  extends PatchOperation<EPerson, String>
        implements ResourcePatchOperation<EPerson> {
    @Override
    public void perform(Context context, EPerson resource, Operation operation)
            throws SQLException, AuthorizeException, PatchBadRequestException {

        replace(context, resource, operation);
    }

    private void replace(Context context, EPerson eperson, Operation operation)
            throws PatchBadRequestException, SQLException, AuthorizeException {

        Boolean requireCert = BooleanUtils.toBooleanObject((String) operation.getValue());

        if (requireCert == null) {
            // this check can be probably moved in the AbstractResourcePatch class as it is mandate by the json+patch
            // specification
            throw new PatchBadRequestException("Boolean value not provided for certificate operation.");
        }
        eperson.setRequireCertificate(requireCert);

    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return null;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return null;
    }
}
