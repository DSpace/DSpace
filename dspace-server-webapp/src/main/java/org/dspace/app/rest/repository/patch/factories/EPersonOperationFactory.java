/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.repository.patch.factories.impl.EPersonCertificateReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.EPersonEmailReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.EPersonLoginReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.EPersonNetidReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.EPersonPasswordReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides factory methods for obtaining instances of eperson patch operations.
 *
 * @author Michael Spalti
 */
@Component
public class EPersonOperationFactory {

    @Autowired
    EPersonPasswordReplaceOperation passwordReplaceOperation;

    @Autowired
    EPersonLoginReplaceOperation loginReplaceOperation;

    @Autowired
    EPersonCertificateReplaceOperation certificateReplaceOperation;

    @Autowired
    EPersonNetidReplaceOperation netIdReplaceOperation;

    @Autowired
    EPersonEmailReplaceOperation emailReplaceOperation;

    public static final String OPERATION_PASSWORD_CHANGE = "/password";
    public static final String OPERATION_CAN_LOGIN = "/canLogin";
    public static final String OPERATION_REQUIRE_CERTIFICATE = "/certificate";
    public static final String OPERATION_SET_NETID = "/netid";
    public static final String OPERATION_SET_EMAIL = "/email";
    /**
     * Returns the patch instance for the replace operation (based on the operation path).
     *
     * @param path the operation path
     * @return the patch operation implementation
     * @throws DSpaceBadRequestException
     */
    public ResourcePatchOperation<EPersonRest> getReplaceOperationForPath(String path) {

        switch (path) {
            case OPERATION_PASSWORD_CHANGE:
                return passwordReplaceOperation;
            case OPERATION_CAN_LOGIN:
                return loginReplaceOperation;
            case OPERATION_REQUIRE_CERTIFICATE:
                return certificateReplaceOperation;
            case OPERATION_SET_NETID:
                return netIdReplaceOperation;
            case OPERATION_SET_EMAIL:
                return emailReplaceOperation;
            default:
                throw new DSpaceBadRequestException("Missing patch operation for: " + path);
        }
    }

}
