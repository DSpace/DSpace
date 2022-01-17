/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "remove" operation to remove custom resource policies.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionRemovePatchOperation extends RemovePatchOperation<AccessConditionDTO> {

    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Override
    void remove(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {

        // "path" : "/sections/<:name-of-the-form>/accessConditions/0"
        // "abspath" : "/accessConditions/0"
        String[] absolutePath = getAbsolutePath(path).split("/");
        Item item = source.getItem();

        if (absolutePath.length == 1) {
            // reset the access condition to the empty array
            authorizeService.removePoliciesActionFilter(context, item, Constants.READ);
        } else if (absolutePath.length == 2) {
            // to remove an access condition
            // contains "<:access-idx>"
            Integer idxToDelete = null;
            try {
                idxToDelete = Integer.parseInt(absolutePath[1]);
            } catch (NumberFormatException e) {
                throw new UnprocessableEntityException("The provided index format is not correct! Must be a number!");
            }

            List<ResourcePolicy> policies = resourcePolicyService.find(context, item, ResourcePolicy.TYPE_CUSTOM);
            if (idxToDelete < 0 || idxToDelete >= policies.size()) {
                throw new UnprocessableEntityException("The provided index:" + idxToDelete + " is not supported,"
                        + " currently the are " + policies.size() + " access conditions");
            }

            ResourcePolicy resourcePolicyToDelete = policies.get(idxToDelete);
            item.getResourcePolicies().remove(resourcePolicyToDelete);
            context.commit();
            resourcePolicyService.delete(context, resourcePolicyToDelete);
        } else {
            throw new UnprocessableEntityException("The patch operation for path:" + path + " is not supported!");
        }
    }

    @Override
    protected Class<AccessConditionDTO[]> getArrayClassForEvaluation() {
        return AccessConditionDTO[].class;
    }

    @Override
    protected Class<AccessConditionDTO> getClassForEvaluation() {
        return AccessConditionDTO.class;
    }

}