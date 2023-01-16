/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.deduplication.model.DuplicateDecisionType;
import org.dspace.app.deduplication.utils.DedupUtils;
import org.dspace.app.deduplication.utils.DuplicateItemInfo;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.factory.ConverterServiceFactoryImpl;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataDetectDuplicate;
import org.dspace.app.rest.model.step.DuplicateMatch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;

/**
 * Describe step for DSpace Spring Rest. Handle the exposition of metadata own
 * by the in progress submission.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
public class DetectPotentialDuplicateStep extends AbstractProcessingStep {

    public static final String DETECT_DUPLICATE_STEP_ADD_OPERATION_ENTRY = "detectduplicateadd";

    private final ConverterService converter = ConverterServiceFactoryImpl.getInstance().getConverterService();

    @Override
    public DataDetectDuplicate getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config) throws Exception {

        DedupUtils dedupUtils = new DSpace().getServiceManager().getServiceByName("dedupUtils", DedupUtils.class);

        UUID itemID = obj.getItem().getID();
        int typeID = obj.getItem().getType();
        boolean isInWorkflow = !(obj instanceof WorkspaceItem);

        List<DuplicateItemInfo> potentialDuplicates = dedupUtils.getDuplicateByIDandType(getContext(), itemID, typeID,
                isInWorkflow);

        Map<UUID, DuplicateMatch> matches = processPotentialDuplicates(itemID, isInWorkflow, potentialDuplicates);
        DataDetectDuplicate result = new DataDetectDuplicate();
        if (!matches.isEmpty()) {
            result.setMatches(matches);
        }

        return result;
    }

    private Map<UUID, DuplicateMatch> processPotentialDuplicates(UUID itemID, boolean check,
            List<DuplicateItemInfo> potentialDuplicates) {
        Map<UUID, DuplicateMatch> matches = new HashMap<UUID, DuplicateMatch>();
        Context context = org.dspace.web.ContextUtil.obtainCurrentRequestContext();
        context.turnOffAuthorisationSystem();
        for (DuplicateItemInfo itemInfo : potentialDuplicates) {
            DuplicateMatch match = new DuplicateMatch();
            Item duplicateItem = (Item)itemInfo.getDuplicateItem();
            match.setMatchObject(converter.toRest(duplicateItem, Projection.DEFAULT));
            match.setSubmitterDecision(itemInfo.getDecision(DuplicateDecisionType.WORKSPACE));
            match.setWorkflowDecision(itemInfo.getDecision(DuplicateDecisionType.WORKFLOW));
            match.setAdminDecision(itemInfo.getDecision(DuplicateDecisionType.ADMIN));
            match.setSubmitterNote(itemInfo.getNote(DuplicateDecisionType.WORKSPACE));
            match.setWorkflowNote(itemInfo.getNote(DuplicateDecisionType.WORKFLOW));

            // avoid a clash with data with a valid decision
            if (!matches.containsKey(duplicateItem.getID()) || match.anyDecisionMade()) {
                matches.put(duplicateItem.getID(), match);
            }
        }
        context.restoreAuthSystemState();

        return matches;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
        Operation op,
        SubmissionStepConfig stepConf) throws Exception {
        PatchOperation<MetadataValueRest> patchOperation = new PatchOperationFactory()
                .instanceOf(DETECT_DUPLICATE_STEP_ADD_OPERATION_ENTRY, op.getOp());
        patchOperation.perform(context, currentRequest, source, op);
    }

    private Context getContext() {
        Context context = null;
        Request currentRequest = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }

        return context;
    }
}