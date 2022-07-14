/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.deduplication.model.DuplicateDecisionObjectRest;
import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.LateObjectEvaluator;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

/**
 * Submission "add" PATCH operation. This records a decision made about whether a potential duplicate
 * is truly a duplicate of the item being submitted or reviewed
 *
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

    /**
     * Add a duplicate decision to the database and index to Solr.
     *
     * @param context
     * @param currentRequest
     * @param source
     * @param path
     * @param value
     * @throws Exception
     */
    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path, Object value)
            throws Exception {

        // Split relative REST URI path into segments and reject if an invalid length
        String[] pathSegments = getAbsolutePath(path).split("/");
        if ((pathSegments.length != 3) || (pathSegments[0].compareTo("matches") != 0)) {
            throw new IllegalArgumentException(
                    String.format("The specified path '%s' is not valid", getAbsolutePath(path)));
        }

        // Get deduplication utilities
        DedupUtils dedupUtils = new DSpace().getServiceManager().getServiceByName("dedupUtils", DedupUtils.class);

        // Evaluate patch value as a decision object
        DuplicateDecisionObjectRest decisionObject = evaluateSingleObject((LateObjectEvaluator) value);

        // Get item ID of current in-progress item
        UUID currentItemID = source.getItem().getID();

        // Get UUID from the 2nd path segment, throwing an exception if a valid UUID could not be parsed
        UUID duplicateItemID;
        try {
            duplicateItemID = UUID.fromString(pathSegments[1]);
        } catch (IllegalArgumentException ie) {
            throw new UnprocessableEntityException(String.format("The patch item id (%s) is not an UUID",
                    pathSegments[1]));
        }
        // If the item is not a workspace item, it must be in workflow
        boolean isInWorkflow = !(source instanceof WorkspaceItem);

        // Get the type of decision from the last path segment
        String decisionType = pathSegments[2];

        // Get the item resource type
        Integer resourceType = source.getItem().getType();

        switch (decisionType) {
            // A submitter made this decision. Set or validate decision type.
            case "submitterDecision":
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.WORKSPACE);
                    // Throw an exception if the item is null or not in workspace
                    if (!(source instanceof WorkspaceItem)) {
                        throw new UnprocessableEntityException(
                                String.format("The specified path %s can be used only with a workspace item",
                                        decisionType));
                    }
                // Throw an exception if the existing decision object type doesn't match the decision type
                } else if (decisionObject.getType() != DuplicateDecisionType.WORKSPACE) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not WORKSPACE", decisionType));
                }
                break;
            case "workflowDecision":
                // A reviewer or editor made this decision. Set or validate decision type.
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.WORKFLOW);
                    // Throw an exception if the item is null or not in workflow
                    if (!(source instanceof WorkflowItem)) {
                        throw new UnprocessableEntityException(
                                String.format("The specified path %s can be used only with a workflow item",
                                        decisionType));
                    }
                // Throw an exception if the existing decision object type doesn't match the decision type
                } else if (decisionObject.getType() != DuplicateDecisionType.WORKFLOW) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not WORKFLOW", decisionType));
                }
                break;
            case "adminDecision":
                // An administrator made this decision. Set the decision type. The item may be in workflow OR workspace.
                if (decisionObject.getType() == null) {
                    decisionObject.setType(DuplicateDecisionType.ADMIN);
                    // Throw an exception if the existing decision object type doesn't match the decision type
                } else if (decisionObject.getType() != DuplicateDecisionType.ADMIN) {
                    throw new UnprocessableEntityException(
                            String.format("The specified path %s is not ADMIN", decisionType));
                }
                break;
            default:
                throw new UnprocessableEntityException(String.format("The specified path %s is not valid",
                        decisionType));
        }

        // Generate an UnprocessableEntityException if the decisionObject is invalid
        try {
            if (!dedupUtils.validateDecision(decisionObject)) {
                throw new UnprocessableEntityException(
                        String.format("The specified decision %s is not valid", decisionObject.getValue()));
            }
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException(String.format("The specified decision %s is not valid",
                    decisionType));
        }

        // Generate an UnprocessableEntityException if the target item for this decisionObject can't be found
        if (!dedupUtils.matchExist(context, currentItemID, duplicateItemID, resourceType, isInWorkflow)) {
            throw new UnprocessableEntityException(
                    String.format("Cannot find any duplicate match related to Item %s", duplicateItemID));
        }

        // Finally, set the duplicate decision in database and update Solr index
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