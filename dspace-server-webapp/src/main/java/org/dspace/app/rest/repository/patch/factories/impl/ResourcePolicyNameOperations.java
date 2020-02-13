/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy name patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /name", "value": "New Name"]'
 * </code>
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyNameOperations implements ResourcePatchOperation<ResourcePolicyRest> {

    @Override
    public ResourcePolicyRest perform(ResourcePolicyRest resource, Operation operation)
            throws DSpaceBadRequestException {
        switch (operation.getOp()) {
            case "replace":
                checkOperationValue(operation.getValue());
                checkModelForExistingValue(resource, operation);
                return replace(resource, operation);
            case "add":
                checkOperationValue(operation.getValue());
                checkModelForNotExistingValue(resource, operation);
                return add(resource, operation);
            case "remove":
                checkModelForExistingValue(resource, operation);
                return delete(resource, operation);
            default:
                throw new DSpaceBadRequestException("Unsupported operation " + operation.getOp());
        }
    }

    public ResourcePolicyRest replace(ResourcePolicyRest resourcePolicy, Operation operation) {
        String newName = (String) operation.getValue();
        resourcePolicy.setName(newName);
        return resourcePolicy;
    }

    public ResourcePolicyRest add(ResourcePolicyRest resourcePolicy, Operation operation) {
        String name = (String) operation.getValue();
        resourcePolicy.setName(name);
        return resourcePolicy;
    }

    public ResourcePolicyRest delete(ResourcePolicyRest resourcePolicy, Operation operation) {
        resourcePolicy.setName(null);
        return resourcePolicy;
    }

    /**
     * Throws PatchBadRequestException for missing operation value.
     *
     * @param value
     *            the value to test
     */
    void checkOperationValue(Object value) {
        if (value == null || value.equals("")) {
            throw new DSpaceBadRequestException("No value provided for the operation.");
        }
    }

    /**
     * Throws PatchBadRequestException for missing value in the /name path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     * 
     */
    void checkModelForExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to " + operation.getOp() + " a non-existent value.");
        }
    }

    /**
     * Throws PatchBadRequestException if a value is already set in the /name path.
     *
     * @param resource
     *            the resource to update
     * @param operation
     *            the operation to apply
     * 
     */
    void checkModelForNotExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getName() != null) {
            throw new DSpaceBadRequestException("Attempting to add a value to an already existing path.");
        }
    }
}
