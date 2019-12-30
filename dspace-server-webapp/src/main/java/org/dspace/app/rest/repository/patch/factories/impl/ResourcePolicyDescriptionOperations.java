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
public class ResourcePolicyDescriptionOperations extends ReplacePatchOperation<ResourcePolicyRest, String>
        implements ResourcePatchOperation<ResourcePolicyRest> {

    @Override
    ResourcePolicyRest replace(ResourcePolicyRest resourcePolicy, Operation operation) {
        resourcePolicy.setDescription((String) operation.getValue());
        return resourcePolicy;
    }


    @Override
    void checkModelForExistingValue(ResourcePolicyRest resource, Operation operation) {
        if (resource.getName() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }
}
