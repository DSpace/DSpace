/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.resourcePolicy;

import static org.dspace.app.rest.repository.patch.operation.resourcePolicy.ResourcePolicyUtils.OPERATION_PATH_ACTION;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy action ADD or REPLACE patch.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /action", "value": 2]'
 * </code>
 *
 * @author Emanuele Ballarini (emanuele.ballarini@4science.com)
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
@Component
public class ResourcePolicyActionAddOrReplaceOperation<R> extends PatchOperation<R> {

    @Override
    public R perform(Context context, R resource, Operation operation) {
        checkOperationValue(operation.getValue());
        if (this.supports(resource, operation)) {
            ResourcePolicy resourcePolicy = (ResourcePolicy) resource;
            this.replace(resourcePolicy, operation);
            return resource;
        } else {
            throw new DSpaceBadRequestException(this.getClass() + " does not support this operation");
        }
    }

    /**
     * Performs the actual add or replace action of resourcePolicy operation. Both
     * actions are allowed since the starting value of action is a defined int.
     * 
     * @param resourcePolicy resourcePolicy being patched
     * @param operation      patch operation
     */
    private void replace(ResourcePolicy resourcePolicy, Operation operation) {
        int action = Constants.getActionID(operation.getValue().toString());
        if (action < 0 || action > Constants.actionText.length) {
            throw new UnprocessableEntityException(action + "is not defined");
        }
        resourcePolicy.setAction(action);
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResourcePolicy
                && (operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                        || operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE))
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_ACTION));
    }
}
