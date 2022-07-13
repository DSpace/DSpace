/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "replace" operation to replace resource policies in the Bitstream
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamResourcePolicyReplacePatchOperation extends ReplacePatchOperation<ResourcePolicyRest> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    UploadConfigurationService uploadConfigurationService;

    @Override
    void replace(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
            Object value) throws Exception {
        // "path": "/sections/upload/files/0/accessConditions/0"
        // "abspath": "/files/0/accessConditions/0"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();

        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

        Collection<UploadConfiguration> uploadConfigsCollection = uploadConfigurationService.getMap().values();
        Iterator<UploadConfiguration> uploadConfigs = uploadConfigsCollection.iterator();
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {
                    List<ResourcePolicy> policies = authorizeService.findPoliciesByDSOAndType(context, b,
                                                                                              ResourcePolicy
                                                                                                  .TYPE_CUSTOM);
                    String rpIdx = split[3];

                    int index = 0;
                    for (ResourcePolicy policy : policies) {
                        int toReplace = Integer.parseInt(rpIdx);
                        if (index == toReplace) {
                            b.getResourcePolicies().remove(policy);
                            break;
                        }
                        index++;
                    }

                    if (split.length == 4) {
                        while (uploadConfigs.hasNext()) {
                            ResourcePolicyRest newAccessCondition = evaluateSingleObject((LateObjectEvaluator) value);

                            String name = newAccessCondition.getName();
                            String description = newAccessCondition.getDescription();
                            Date startDate = newAccessCondition.getStartDate();
                            Date endDate = newAccessCondition.getEndDate();
                            // TODO manage duplicate policy
                            BitstreamResourcePolicyUtils.findApplyResourcePolicy(context, uploadConfigs.next(), b, name,
                                    description, startDate, endDate);
                        }
                    } else {
                        // "path":
                        // "/sections/upload/files/0/accessConditions/0/startDate"
                        // TODO
                    }
                }
                idx++;
            }
        }
    }

    @Override
    protected Class<ResourcePolicyRest[]> getArrayClassForEvaluation() {
        return ResourcePolicyRest[].class;
    }

    @Override
    protected Class<ResourcePolicyRest> getClassForEvaluation() {
        return ResourcePolicyRest.class;
    }

}
