/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.resourcePolicy;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy name REPLACE patch.
 *
 * Example:
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /name", "value": "New Name"]'
 * </code>
 *
 * @author Maria Verdonck (Atmire) on 14/02/2020
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyNameReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    ResourcePolicyUtils resourcePolicyUtils;

    @Override
    public R perform(Context context, R resource, Operation operation) {
        checkOperationValue(operation.getValue());
        if (this.supports(resource, operation)) {
            ResourcePolicy resourcePolicy = (ResourcePolicy) resource;
            resourcePolicyUtils.checkResourcePolicyForExistingNameValue(resourcePolicy, operation);
            this.replace(resourcePolicy, operation);
            return resource;
        } else {
            throw new DSpaceBadRequestException(this.getClass() + " does not support this operation");
        }
    }

    /**
     * Performs the actual replace name of resourcePolicy operation
     * @param resourcePolicy    resourcePolicy being patched
     * @param operation         patch operation
     */
    private void replace(ResourcePolicy resourcePolicy, Operation operation) {
        String newName = (String) operation.getValue();
        resourcePolicy.setRpName(newName);
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResourcePolicy && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
            && operation.getPath().trim().equalsIgnoreCase(resourcePolicyUtils.OPERATION_PATH_NAME));
    }
}
