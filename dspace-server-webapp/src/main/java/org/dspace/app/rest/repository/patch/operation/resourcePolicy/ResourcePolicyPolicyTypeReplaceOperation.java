/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.resourcePolicy;

import static org.dspace.app.rest.repository.patch.operation.resourcePolicy.ResourcePolicyUtils.OPERATION_PATH_POLICY_TYPE;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy policyType REPLACE patch.
 *
 * Example: Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /policyType", "value": "TYPE_CUSTOM"]'
 * </code>
 *
 * @author Emanuele Ballarini (emanuele.ballarini@4science.com)
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
@Component
public class ResourcePolicyPolicyTypeReplaceOperation<R> extends PatchOperation<R> {

    @Override
    public R perform(Context context, R resource, Operation operation) {
        checkOperationValue(operation.getValue());
        if (this.supports(resource, operation)) {
            ResourcePolicy resourcePolicy = (ResourcePolicy) resource;
            this.checkResourcePolicyForExistingPolicyTypeValue(resourcePolicy, operation);
            this.replace(resourcePolicy, operation);
            return resource;
        } else {
            throw new DSpaceBadRequestException(this.getClass() + " does not support this operation");
        }
    }

    /**
     * Performs the actual replace policyType of resourcePolicy operation
     * 
     * @param resourcePolicy resourcePolicy being patched
     * @param operation      patch operation
     */
    private void replace(ResourcePolicy resourcePolicy, Operation operation) {
        String newPolicyType = (String) operation.getValue();
        resourcePolicy.setRpType(newPolicyType);
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResourcePolicy && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_POLICY_TYPE));
    }

    /**
     * Throws DSpaceBadRequestException when attempting to replace a non-existent
     * value in /policyType path
     * 
     * @param resource
     * @param operation
     */
    private void checkResourcePolicyForExistingPolicyTypeValue(ResourcePolicy resource, Operation operation) {
        if (resource.getRpType() == null) {
            throw new DSpaceBadRequestException(
                    "Attempting to " + operation.getOp() + " a non-existent policyType value.");
        }
    }
}
