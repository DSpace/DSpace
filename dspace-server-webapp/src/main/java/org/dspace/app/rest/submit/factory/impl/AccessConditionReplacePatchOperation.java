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
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" operation to replace custom resource policies.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionReplacePatchOperation extends ReplacePatchOperation<AccessConditionDTO> {

    private static final Logger log = LoggerFactory.getLogger(AccessConditionReplacePatchOperation.class);

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
        int toReplace = Integer.parseInt(split[1]);
        List<ResourcePolicy> policies = resourcePolicyService.find(context, item, ResourcePolicy.TYPE_CUSTOM);
        if (toReplace < 0 || toReplace >= policies.size()) {
            throw new UnprocessableEntityException("The provided index:" + toReplace + " is not supported,"
                    + " currently the are " + policies.size() + " access conditions");
        }

        if (split.length == 2) {
            AccessConditionDTO accessConditionDTO = evaluateSingleObject((LateObjectEvaluator) value);
            if (Objects.nonNull(accessConditionDTO) && Objects.nonNull(getOption(configuration, accessConditionDTO))) {
                verifyAccessCondition(context, configuration, accessConditionDTO);
                if (checkDuplication(context, policies, accessConditionDTO, toReplace, item)) {
                    context.commit();
                    resourcePolicyService.delete(context, policies.get(toReplace));
                    AccessConditionOption option = getOption(configuration, accessConditionDTO);
                    option.createPolicy(context, item, accessConditionDTO.getName(), null,
                            accessConditionDTO.getStartDate(),accessConditionDTO.getEndDate());
                }
            }
        } else if (split.length == 3) {
            String valueToReplace = getValue(value);
            String attributeReplace = split[2];
            ResourcePolicy rpToReplace = policies.get(toReplace);
            AccessConditionDTO accessConditionDTO = createDTO(rpToReplace, attributeReplace, valueToReplace);
            verifyAccessCondition(context, configuration, accessConditionDTO);
            updatePolicy(context, valueToReplace, attributeReplace, rpToReplace);
        }
    }

    private void verifyAccessCondition(Context context, AccessConditionConfiguration configuration,
            AccessConditionDTO dto) throws SQLException, AuthorizeException, ParseException {
        AccessConditionResourcePolicyUtils.canApplyResourcePolicy(context, configuration.getOptions(),
                dto.getName(), dto.getStartDate(), dto.getEndDate());
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

    private boolean checkDuplication(Context context, List<ResourcePolicy> policies,
            AccessConditionDTO accessConditionDTO, int toReplace, Item item)
            throws SQLException, AuthorizeException, ParseException {
        ResourcePolicy policyToReplace = policies.get(toReplace);
        // check if the resource policy is of the same type
        if (policyToReplace.getRpName().equals(accessConditionDTO.getName())) {
            item.getResourcePolicies().remove(policyToReplace);
            return true;
        }
        // check if there is not already a policy of the same type
        for (ResourcePolicy resourcePolicy : policies) {
            if (resourcePolicy.getRpName().equals(accessConditionDTO.getName())) {
                return false;
            }
        }
        item.getResourcePolicies().remove(policyToReplace);
        return true;
    }

    private AccessConditionDTO createDTO(ResourcePolicy rpToReplace, String attributeReplace, String valueToReplare)
            throws ParseException {
        AccessConditionDTO accessCondition = new AccessConditionDTO();
        accessCondition.setName(rpToReplace.getRpName());
        accessCondition.setStartDate(rpToReplace.getStartDate());
        accessCondition.setEndDate(rpToReplace.getEndDate());
        switch (attributeReplace) {
            case "name":
                accessCondition.setName(valueToReplare);
                return accessCondition;
            case "startDate":
                accessCondition.setStartDate(parsDate(valueToReplare));
                return accessCondition;
            case "endDate":
                accessCondition.setEndDate(parsDate(valueToReplare));
                return accessCondition;
            default:
                throw new UnprocessableEntityException("The provided attribute: "
                                                       + attributeReplace + " is not supported");
        }
    }

    private void updatePolicy(Context context, String valueToReplare, String attributeReplace,
            ResourcePolicy rpToReplace) throws SQLException, AuthorizeException {
        switch (attributeReplace) {
            case "name":
                rpToReplace.setRpName(valueToReplare);
                break;
            case "startDate":
                rpToReplace.setStartDate(parsDate(valueToReplare));
                break;
            case "endDate":
                rpToReplace.setEndDate(parsDate(valueToReplare));
                break;
            default:
        }
        resourcePolicyService.update(context, rpToReplace);
    }

    private Date parsDate(String date) {
        List<SimpleDateFormat> knownPatterns = Arrays.asList(
                                new SimpleDateFormat("yyyy-MM-dd"),
                                new SimpleDateFormat("dd-MM-yyyy"),
                                new SimpleDateFormat("yyyy/MM/dd"),
                                new SimpleDateFormat("dd/MM/yyyy"));
        for (SimpleDateFormat pattern : knownPatterns) {
            try {
                return pattern.parse(date);
            } catch (ParseException e) {
                log.error(e.getMessage(), e);
            }
        }
        throw new UnprocessableEntityException("Provided format of date:" + date + " is not supported!");
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

    @Override
    protected Class<AccessConditionDTO[]> getArrayClassForEvaluation() {
        return AccessConditionDTO[].class;
    }

    @Override
    protected Class<AccessConditionDTO> getClassForEvaluation() {
        return AccessConditionDTO.class;
    }

}