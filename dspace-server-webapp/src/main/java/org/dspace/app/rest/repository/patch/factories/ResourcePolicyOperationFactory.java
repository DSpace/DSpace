/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePolicyDescriptionOperations;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePolicyEndDateOperations;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePolicyNameOperations;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePolicyStartDateOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides factory methods for obtaining instances of ResourcePolicy patch operations.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyOperationFactory {

    @Autowired
    ResourcePolicyStartDateOperations resourcePolicyStartDateOperations;

    @Autowired
    ResourcePolicyEndDateOperations resourcePolicyEndDateOperations;

    @Autowired
    ResourcePolicyNameOperations resourcePolicyNameOperations;

    @Autowired
    ResourcePolicyDescriptionOperations resourcePolicyDescriptionOperations;

    private static final String OPERATION_PATH_STARTDATE = "/startDate";
    private static final String OPERATION_PATH_ENDDATE = "/endDate";
    private static final String OPERATION_PATH_DESCRIPTION = "/description";
    private static final String OPERATION_PATH_NAME = "/name";

    /**
     * Returns the patch instance for the operation (based on the operation path).
     *
     * @param path the operation path
     * @return the patch operation implementation
     * @throws DSpaceBadRequestException
     */
    public ResourcePatchOperation<ResourcePolicyRest> getOperationForPath(String path) {

        switch (path) {
            case OPERATION_PATH_STARTDATE:
                return resourcePolicyStartDateOperations;
            case OPERATION_PATH_ENDDATE:
                return resourcePolicyEndDateOperations;
            case OPERATION_PATH_DESCRIPTION:
                return resourcePolicyDescriptionOperations;
            case OPERATION_PATH_NAME:
                return resourcePolicyNameOperations;
            default:
                throw new DSpaceBadRequestException("Missing patch operation for: " + path);
        }
    }
}
