/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy endDate patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /endDate", "value": "YYYY-MM-DD"]'
 * </code>
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyEndDateOperations implements ResourcePatchOperation<ResourcePolicyRest> {

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public ResourcePolicyRest perform(ResourcePolicyRest resource, Operation operation)
            throws DSpaceBadRequestException {
        switch (operation.getOp()) {
            case "replace":
                checkOperationValue(operation.getValue());
                checkModelForExistingValue(resource, operation);
                checkModelForConsistentValue(resource, operation);
                return replace(resource, operation);
            case "add":
                checkOperationValue(operation.getValue());
                checkModelForNotExistingValue(resource, operation);
                checkModelForConsistentValue(resource, operation);
                return add(resource, operation);
            case "remove":
                checkModelForExistingValue(resource, operation);
                return delete(resource, operation);
            default:
                throw new DSpaceBadRequestException("Unsupported operation " + operation.getOp());
        }
    }

    ResourcePolicyRest add(ResourcePolicyRest resourcePolicy, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = simpleDateFormat.parse(dateS);
            resourcePolicy.setEndDate(date);
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid endDate value", e);
        }
        return resourcePolicy;
    }

    ResourcePolicyRest delete(ResourcePolicyRest resourcePolicy, Operation operation) {
        resourcePolicy.setEndDate(null);
        return resourcePolicy;
    }

    ResourcePolicyRest replace(ResourcePolicyRest resourcePolicy, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = simpleDateFormat.parse(dateS);
            resourcePolicy.setEndDate(date);
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid endDate value", e);
        }
        return resourcePolicy;
    }

    /**
     * Throws PatchBadRequestException for missing operation value.
     *
     * @param value
     *            the value to test
     */
    void checkOperationValue(Object value) {
        if (value == null) {
            throw new DSpaceBadRequestException("No value provided for the operation.");
        }
    }

    /**
     * Throws PatchBadRequestException for missing value in the /endDate path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     * 
     */
    void checkModelForExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getEndDate() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp() + " a non-existent value.");
        }
    }

    /**
     * Throws PatchBadRequestException if a value is already set in the /endDate path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     * 
     */
    void checkModelForNotExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getEndDate() != null) {
            throw new DSpaceBadRequestException("Attempting to add a value to an already existing path.");
        }
    }

    /**
     * Throws PatchBadRequestException if the value for endDate is not consistent with the startDate value, if present
     * (smaller than).
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     * 
     */
    void checkModelForConsistentValue(ResourcePolicyRest resource, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = simpleDateFormat.parse(dateS);
            if (resource.getEndDate() != null && resource.getStartDate().after(date)) {
                throw new DSpaceBadRequestException("Attempting to set an invalid endDate smaller than the startDate.");
            }
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid endDate value", e);
        }
    }
}
