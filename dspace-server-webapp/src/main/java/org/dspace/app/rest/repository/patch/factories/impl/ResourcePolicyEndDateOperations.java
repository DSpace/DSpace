/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

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
public class ResourcePolicyEndDateOperations extends ReplacePatchOperation<ResourcePolicyRest, Date>
        implements ResourcePatchOperation<ResourcePolicyRest> {

    @Override
    ResourcePolicyRest replace(ResourcePolicyRest resourcePolicy, Operation operation) {
        resourcePolicy.setEndDate((Date) operation.getValue());
        return resourcePolicy;
    }


    @Override
    void checkModelForExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getEndDate() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
        if (resource.getStartDate() != null && resource.getStartDate().after((Date) operation.getValue())) {
            throw new DSpaceBadRequestException("Attempting to set an invalid startDate greater than the endDate.");
        }
    }

    @Override
    protected Class<Date[]> getArrayClassForEvaluation() {
        return Date[].class;
    }

    @Override
    protected Class<Date> getClassForEvaluation() {
        return Date.class;
    }
}
