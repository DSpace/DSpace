/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dspace.app.rest.model.UploadBitstreamAccessConditionDTO;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "add" operation to add resource policies in the Bitstream
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class BitstreamResourcePolicyAddPatchOperation extends AddPatchOperation<UploadBitstreamAccessConditionDTO> {


    @Autowired
    ItemService itemService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    UploadConfigurationService uploadConfigurationService;

    @Override
    void add(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        //"path": "/sections/upload/files/0/accessConditions"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();

        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        ;
        Collection<UploadConfiguration>  uploadConfigsCollection = uploadConfigurationService.getMap().values();
        Iterator<UploadConfiguration> uploadConfigs = uploadConfigsCollection.iterator();
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {

                    List<UploadBitstreamAccessConditionDTO> newAccessConditions =
                                                            new ArrayList<UploadBitstreamAccessConditionDTO>();
                    if (split.length == 3) {
                        authorizeService.removePoliciesActionFilter(context, b, Constants.READ);
                        newAccessConditions = evaluateArrayObject((LateObjectEvaluator) value);
                    } else if (split.length == 4) {
                        // contains "-", call index-based accessConditions it make not sense
                        newAccessConditions.add(evaluateSingleObject((LateObjectEvaluator) value));
                    }

                    for (UploadBitstreamAccessConditionDTO newAccessCondition : newAccessConditions) {
                        // TODO manage duplicate policy
                        BitstreamResourcePolicyUtils.findApplyResourcePolicy(context, uploadConfigs,
                                b, newAccessCondition);
                    }
                }
                idx++;
            }
        }
    }

    @Override
    protected Class<UploadBitstreamAccessConditionDTO[]> getArrayClassForEvaluation() {
        return UploadBitstreamAccessConditionDTO[].class;
    }

    @Override
    protected Class<UploadBitstreamAccessConditionDTO> getClassForEvaluation() {
        return UploadBitstreamAccessConditionDTO.class;
    }
}
