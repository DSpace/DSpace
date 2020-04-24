/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.UUID;

import org.dspace.app.deduplication.model.DuplicateDecisionObjectRest;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

/**
 * Submission "add" PATCH operation.
 * <p>
 * With path:<br>
 * matches/<UUID>/submitterDecision or matches/<UUID>/workflowDecision
 * <p>
 * With body:<br>
 * value: reject or verify and note: null or <description>.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
public class DetectDuplicateAddPatchOperation extends AddPatchOperation<DuplicateDecisionObjectRest> {

    @Override
    void add(Context context, Request currentRequest, InProgressSubmission source, String path, Object value)
            throws Exception {
        String[] split = getAbsolutePath(path).split("/");
        if ((split.length != 3) || (split[0].compareTo("matches") != 0)) {
            throw new IllegalArgumentException(
                    String.format("The specified path '%s' is not valid", getAbsolutePath(path)));
        }

        DedupUtils dedupUtils = new DSpace().getServiceManager().getServiceByName("dedupUtils", DedupUtils.class);

        DuplicateDecisionObjectRest decisionObject = evaluateSingleObject((LateObjectEvaluator) value);
        UUID currentItemID = source.getItem().getID();
        UUID duplicateItemID = null;
        try {
            duplicateItemID = UUID.fromString(split[1]);
        } catch (IllegalArgumentException ie) {
            throw new UnprocessableEntityException(String.format("The patch item id (%s) is not an UUID", split[1]));
        }
        boolean isInWorkflow = !(source instanceof WorkspaceItem);
        String subPath = split[2];
        Integer resourceType = source.getItem().getType();

        switch (subPath) {
            case "submitterDecision":
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.WORKSPACE);

                    if (!(source instanceof WorkspaceItem)) {
                        throw new UnprocessableEntityException(
                                String.format("The specified path %s can be used only with a workspace item", subPath));
                    }
                } else if (decisionObject.getType() != DuplicateDecisionType.WORKSPACE) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not WORKSPACE", subPath));
                }
                break;
            case "workflowDecision":
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.WORKFLOW);

                    if (!(source instanceof WorkflowItem)) {
                        throw new UnprocessableEntityException(
                                String.format("The specified path %s can be used only with a workflow item", subPath));
                    }
                } else if (decisionObject.getType() != DuplicateDecisionType.WORKFLOW) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not WORKFLOW", subPath));
                }
                break;
            case "adminDecision":
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.ADMIN);
                } else if (decisionObject.getType() != DuplicateDecisionType.ADMIN) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not ADMIN", subPath));
                }
                break;
            default:
                throw new UnprocessableEntityException(String.format("The specified path %s is not valid", subPath));
        }

        // generate UnprocessableEntityException if decisionObject is invalid
        try {
            if (!dedupUtils.validateDecision(decisionObject)) {
                throw new UnprocessableEntityException(
                        String.format("The specified decision %s is not valid", decisionObject.getValue()));
            }
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException(String.format("The specified decision %s is not valid", subPath));
        }

        if (!dedupUtils.matchExist(context, currentItemID, duplicateItemID, resourceType, null, isInWorkflow)) {
            throw new UnprocessableEntityException(
                    String.format("Cannot find any duplicate match related to Item %s", duplicateItemID));
        }

        dedupUtils.setDuplicateDecision(context, source.getItem().getID(), duplicateItemID, source.getItem().getType(),
                decisionObject);

    }

    @Override
    protected Class<DuplicateDecisionObjectRest[]> getArrayClassForEvaluation() {
        return DuplicateDecisionObjectRest[].class;
    }

    @Override
    protected Class<DuplicateDecisionObjectRest> getClassForEvaluation() {
        return DuplicateDecisionObjectRest.class;
    }

}