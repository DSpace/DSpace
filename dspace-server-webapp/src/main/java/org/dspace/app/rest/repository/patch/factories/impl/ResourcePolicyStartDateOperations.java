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
 * Implementation for ResourcePolicy startDate patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/authz/resourcepolicies/<:id-resourcepolicy> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /startDate", "value": "YYYY-MM-DD"]'
 * </code>
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ResourcePolicyStartDateOperations extends ReplacePatchOperation<ResourcePolicyRest, Date>
        implements ResourcePatchOperation<ResourcePolicyRest> {

    @Override
    ResourcePolicyRest replace(ResourcePolicyRest resourcePolicy, Operation operation) {
        resourcePolicy.setStartDate((Date) operation.getValue());
        return resourcePolicy;
    }


    @Override
    void checkModelForExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getStartDate() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
        if (resource.getEndDate() != null && resource.getEndDate().before((Date) operation.getValue())) {
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
