/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.resourcePolicy;

import java.text.ParseException;
import java.util.Date;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResourcePolicy  endDate REPLACE patch.
 *
 * Example:
 * <code>
 * curl -X PATCH http://${dspace.server.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /endDate", "value": "YYYY-MM-DD"]'
 * </code>
 *
 * @author Maria Verdonck (Atmire) on 14/02/2020
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyEndDateReplaceOperation<R> extends PatchOperation<R> {

    @Autowired
    ResourcePolicyUtils resourcePolicyUtils;

    @Override
    public R perform(Context context, R resource, Operation operation) {
        checkOperationValue(operation.getValue());
        if (this.supports(resource, operation)) {
            ResourcePolicy resourcePolicy = (ResourcePolicy) resource;
            resourcePolicyUtils.checkResourcePolicyForExistingEndDateValue(resourcePolicy, operation);
            resourcePolicyUtils.checkResourcePolicyForConsistentEndDateValue(resourcePolicy, operation);
            this.replace(resourcePolicy, operation);
            return resource;
        } else {
            throw new DSpaceBadRequestException(this.getClass() + " does not support this operation");
        }
    }

    /**
     * Performs the actual replace endDate of resourcePolicy operation
     * @param resourcePolicy    resourcePolicy being patched
     * @param operation         patch operation
     */
    private void replace(ResourcePolicy resourcePolicy, Operation operation) {
        String dateS = (String) operation.getValue();
        try {
            Date date = resourcePolicyUtils.simpleDateFormat.parse(dateS);
            resourcePolicy.setEndDate(date);
        } catch (ParseException e) {
            throw new DSpaceBadRequestException("Invalid endDate value", e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResourcePolicy && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
            && operation.getPath().trim().equalsIgnoreCase(resourcePolicyUtils.OPERATION_PATH_ENDDATE));
    }
}
