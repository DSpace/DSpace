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
        AccessConditionConfiguration configuration = accessConditionConfigurationService
                .getAccessConfigurationById(stepId);

        // "path" : "/sections/<:name-of-the-form>/accessConditions/0/name"
        // the absolutePath will be : accessConditions/0 or accessConditions/0/name
        String[] absolutePath = getAbsolutePath(path).split("/");
        Item item = source.getItem();
        Integer idxToReplace = null;
        try {
            idxToReplace = Integer.parseInt(absolutePath[1]);
        } catch (NumberFormatException e) {
            throw new UnprocessableEntityException("The provided index format is not correct! Must be a number!");
        }
        List<ResourcePolicy> policies = resourcePolicyService.find(context, item, ResourcePolicy.TYPE_CUSTOM);
        if (idxToReplace < 0 || idxToReplace >= policies.size()) {
            throw new UnprocessableEntityException("The provided index:" + idxToReplace + " is not supported,"
                    + " currently the are " + policies.size() + " access conditions");
        }

        if (absolutePath.length == 2) {
            // "/sections/<:name-of-the-form>/accessConditions/0"
            // to replace an access condition with a new one
            AccessConditionDTO accessConditionDTO = evaluateSingleObject((LateObjectEvaluator) value);
            if (Objects.nonNull(accessConditionDTO) && Objects.nonNull(getOption(configuration, accessConditionDTO))) {
                item.getResourcePolicies().remove(policies.get(idxToReplace));
                resourcePolicyService.delete(context, policies.get(idxToReplace));
                AccessConditionOption option = getOption(configuration, accessConditionDTO);
                option.createResourcePolicy(context, item, accessConditionDTO.getName(), null,
                                            accessConditionDTO.getStartDate(),accessConditionDTO.getEndDate());
            }
        } else if (absolutePath.length == 3) {
            // "/sections/<:name-of-the-form>/accessConditions/0/startDate"
            // to update the embargo start|end date or changing the policy name
            String valueToReplace = getValue(value);
            String attributeReplace = absolutePath[2];
            ResourcePolicy rpToReplace = policies.get(idxToReplace);
            AccessConditionDTO accessConditionDTO = createDTO(rpToReplace, attributeReplace, valueToReplace);
            updatePolicy(context, valueToReplace, attributeReplace, rpToReplace);
            getOption(configuration, accessConditionDTO).updateResourcePolicy(context, rpToReplace);
        } else {
            throw new UnprocessableEntityException("The patch operation for path:" + path + " is not supported!");
        }
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
                accessCondition.setStartDate(parseDate(valueToReplare));
                return accessCondition;
            case "endDate":
                accessCondition.setEndDate(parseDate(valueToReplare));
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
                rpToReplace.setStartDate(parseDate(valueToReplare));
                break;
            case "endDate":
                rpToReplace.setEndDate(parseDate(valueToReplare));
                break;
            default:
                throw new IllegalArgumentException("Attribute to replace is not valid:" + attributeReplace);
        }
    }

    private Date parseDate(String date) {
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