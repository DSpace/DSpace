/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission "add" operation to add resource policies in the Bitstream
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ResourcePolicyAddPatchOperation extends AddPatchOperation<ResourcePolicyRest> {

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    GroupService groupService;
    @Autowired
    EPersonService epersonService;

    @Override
    void add(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
        throws Exception {
        //"path": "/sections/upload/files/0/accessConditions"
        String[] split = getAbsolutePath(path).split("/");
        Item item = source.getItem();

        List<Bundle> bundle = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        ;
        for (Bundle bb : bundle) {
            int idx = 0;
            for (Bitstream b : bb.getBitstreams()) {
                if (idx == Integer.parseInt(split[1])) {

                    List<ResourcePolicyRest> newAccessConditions = new ArrayList<ResourcePolicyRest>();
                    if (split.length == 3) {
                        authorizeService.removePoliciesActionFilter(context, b, Constants.READ);
                        newAccessConditions = evaluateArrayObject((LateObjectEvaluator) value);
                    } else if (split.length == 4) {
                        // contains "-", call index-based accessConditions it make not sense
                        newAccessConditions.add(evaluateSingleObject((LateObjectEvaluator) value));
                    }

                    for (ResourcePolicyRest newAccessCondition : newAccessConditions) {
                        String name = newAccessCondition.getName();
                        String description = newAccessCondition.getDescription();

                        //TODO manage error on select group and eperson
                        Group group = null;
                        if (newAccessCondition.getGroupUUID() != null) {
                            group = groupService.find(context, newAccessCondition.getGroupUUID());
                        }
                        EPerson eperson = null;
                        if (newAccessCondition.getEpersonUUID() != null) {
                            eperson = epersonService.find(context, newAccessCondition.getEpersonUUID());
                        }

                        Date startDate = newAccessCondition.getStartDate();
                        Date endDate = newAccessCondition.getEndDate();
                        authorizeService.createResourcePolicy(context, b, group, eperson, Constants.READ,
                                                              ResourcePolicy.TYPE_CUSTOM, name, description, startDate,
                                                              endDate);
                        // TODO manage duplicate policy
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
