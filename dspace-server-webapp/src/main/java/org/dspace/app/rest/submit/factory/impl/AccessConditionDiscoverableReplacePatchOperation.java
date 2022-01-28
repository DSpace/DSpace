/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.dspace.submit.model.AccessConditionConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" PATCH operation to change a previous discoverable flag value.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AccessConditionDiscoverableReplacePatchOperation extends ReplacePatchOperation<String> {

    @Autowired
    private AccessConditionConfigurationService accessConditionConfigurationService;

    @Override
    void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String string,
            Object value) throws Exception {

        String stepId = (String) currentRequest.getAttribute("accessConditionSectionId");
        AccessConditionConfiguration configuration = accessConditionConfigurationService
                .getAccessConfigurationById(stepId);

        if (Objects.isNull(configuration) || !configuration.getCanChangeDiscoverable().booleanValue()) {
            throw new UnprocessableEntityException("The current access configurations does not allow" +
                                                   " the user to specify the visibility of the item");
        }

        Boolean discoverable;
        if (value instanceof String) {
            discoverable = BooleanUtils.toBooleanObject((String) value);
        } else {
            discoverable = (Boolean) value;
        }

        if (Objects.isNull(discoverable)) {
            throw new UnprocessableEntityException(
                "Value is not a valid boolean expression permitted value: true|false");
        }

        Item item = source.getItem();

        if (discoverable == item.isDiscoverable()) {
            return;
        }
        item.setDiscoverable(discoverable);
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