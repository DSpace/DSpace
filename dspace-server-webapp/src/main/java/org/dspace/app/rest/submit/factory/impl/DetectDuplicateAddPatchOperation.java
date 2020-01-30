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
import org.dspace.app.rest.exception.PatchUnprocessableEntityException;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;

/**
 * Submission "add" PATCH operation.
 *
 * Path used to add a new value to an <b>existent metadata</b>:
 * "/sections/<:name-of-the-form>/<:metadata>/-"
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
        UUID duplicateItemID = UUID.fromString(split[1]);
        boolean isInWorkflow = !(source instanceof WorkspaceItem);
        String subPath = split[2];
        Integer resourceType = source.getItem().getType();

        switch (subPath) {
            case "submitterDecision":
                decisionObject.setType(DuplicateDecisionType.WORKSPACE);
                break;
            case "workflowDecision":
                decisionObject.setType(DuplicateDecisionType.WORKFLOW);
                break;
            case "adminDecision":
                decisionObject.setType(DuplicateDecisionType.ADMIN);
                break;
            default:
                throw new IllegalArgumentException(String.format("The specified path %s is not valid", subPath));
        }

        if (!dedupUtils.validateDecision(decisionObject)) {
            throw new IllegalArgumentException(
                    String.format("The specified decision %s is not valid", decisionObject.getValue()));
        }

        if (!dedupUtils.matchExist(context, currentItemID, duplicateItemID, resourceType, null, isInWorkflow)) {
            throw new PatchUnprocessableEntityException(
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