/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.dspace.submit.model.AccessConditionConfigurationService;
import org.dspace.submit.model.AccessConditionOption;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" operation to replace custom resource policies.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionReplacePatchOperation extends ReplacePatchOperation<AccessConditionDTO> {

    @Autowired
    private ResourcePolicyService resourcePolicyService;
    @Autowired
    private AccessConditionConfigurationService accessConditionConfigurationService;

    @Override
    void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {

        String stepId = (String) currentRequest.getAttribute("accessConditionSectionId");
        AccessConditionConfiguration configuration = accessConditionConfigurationService.getMap().get(stepId);

        // "path" : "/sections/<:name-of-the-form>/accessConditions/0"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();
        if (split.length == 2) {
            int toReplace = Integer.parseInt(split[1]);
            AccessConditionDTO accessConditionDTO = evaluateSingleObject((LateObjectEvaluator) value);
            if (Objects.nonNull(accessConditionDTO) && Objects.nonNull(getOption(configuration, accessConditionDTO))) {
                List<ResourcePolicy> policies = resourcePolicyService.find(context, item, ResourcePolicy.TYPE_CUSTOM);
                if ((toReplace < 0 || toReplace >= policies.size()) && policies.isEmpty()) {
                    throw new UnprocessableEntityException("The provided index:" + toReplace + " is not supported,"
                            + " currently the are " + policies.size() + " access conditions");
                }
                if (checkDubblication(context, configuration, policies, accessConditionDTO, toReplace, item)) {
                    context.commit();
                    resourcePolicyService.delete(context, policies.get(toReplace));
                    getOption(configuration, accessConditionDTO).createPolicy(context, item,
                            accessConditionDTO.getName(), null, accessConditionDTO.getStartDate(),
                            accessConditionDTO.getEndDate());
                }
            }
        } else if (split.length == 3) {
            String valueToReplare = getValue(value);
            int toReplace = Integer.parseInt(split[1]);
            String attributeReplace = split[2];
            List<ResourcePolicy> policies = resourcePolicyService.find(context, item, ResourcePolicy.TYPE_CUSTOM);
            if ((toReplace < 0 || toReplace >= policies.size()) && policies.isEmpty()) {
                throw new UnprocessableEntityException("The provided index:" + toReplace + " is not supported,"
                        + " currently the are " + policies.size() + " access conditions");
            }
            ResourcePolicy rpToReplace = policies.get(toReplace);
            AccessConditionDTO accessConditionDTO = createDTO(rpToReplace, attributeReplace, valueToReplare);
            boolean canApplay = AccessConditionResourcePolicyUtils.canApplyResourcePolicy(context,
                    configuration.getOptions(), accessConditionDTO.getName(), accessConditionDTO.getStartDate(),
                    accessConditionDTO.getEndDate());
            if (canApplay) {
                switch (attributeReplace) {
                    case "name":
                        rpToReplace.setRpName(valueToReplare);
                        break;
                    case "startDate":
                        rpToReplace.setStartDate(new Date(valueToReplare));
                        break;
                    case "endDate":
                        rpToReplace.setEndDate(new Date(valueToReplare));
                        break;
                    default:
                }
            }
            resourcePolicyService.update(context, rpToReplace);
        }
    }

    private AccessConditionDTO createDTO(ResourcePolicy rpToReplace, String attributeReplace, String valueToReplare) {
        AccessConditionDTO accessCondition = new AccessConditionDTO();
        switch (attributeReplace) {
            case "name":
                accessCondition.setName(valueToReplare);
                accessCondition.setStartDate(rpToReplace.getStartDate());
                accessCondition.setEndDate(rpToReplace.getEndDate());
                return accessCondition;
            case "startDate":
                accessCondition.setName(rpToReplace.getRpName());
                accessCondition.setStartDate(new Date(valueToReplare));
                accessCondition.setEndDate(rpToReplace.getEndDate());
                return accessCondition;
            case "endDate":
                accessCondition.setName(rpToReplace.getRpName());
                accessCondition.setStartDate(rpToReplace.getStartDate());
                accessCondition.setEndDate(new Date(valueToReplare));
                return accessCondition;
            default:
                throw new UnprocessableEntityException("The provided attribute: "
                                                       + attributeReplace + " is not supported");
        }
    }

    private String getValue(Object value) {
        if (value instanceof JsonValueEvaluator) {
            JsonValueEvaluator jsonValue = (JsonValueEvaluator) value;
            if (jsonValue.getValueNode().fields().hasNext()) {
                return jsonValue.getValueNode().fields().next().getValue().asText();
            }
        }
        return StringUtils.EMPTY;
    }
    private AccessConditionOption getOption(AccessConditionConfiguration configuration,
            AccessConditionDTO accessConditionDTO) {
        for (AccessConditionOption option :configuration.getOptions()) {
            if (option.getName().equals(accessConditionDTO.getName())) {
                return option;
            }
        }
        return null;
    }

    private boolean checkDubblication(Context context, AccessConditionConfiguration configuration,
            List<ResourcePolicy> policies, AccessConditionDTO accessConditionDTO, int toReplace, Item item)
            throws SQLException, AuthorizeException, ParseException {
        ResourcePolicy rp = policies.get(toReplace);
        if (rp.getRpName().equals(accessConditionDTO.getName())) {
            boolean canApplay = AccessConditionResourcePolicyUtils.canApplyResourcePolicy(context,
                                      configuration.getOptions(), accessConditionDTO.getName(),
                                      accessConditionDTO.getStartDate(), accessConditionDTO.getEndDate());
            if (canApplay) {
                item.getResourcePolicies().remove(rp);
                return true;
            }
        }
        for (ResourcePolicy resourcePolicy : policies) {
            if (resourcePolicy.getRpName().equals(accessConditionDTO.getName())) {
                return false;
            }
        }
        item.getResourcePolicies().remove(rp);
        return true;
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