/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import static java.util.Optional.of;
import static org.dspace.app.rest.repository.patch.operation.PatchOperation.OPERATION_REMOVE;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.CustomUrl;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link org.dspace.app.rest.submit.DataProcessingStep} that expose and allow patching
 * the custom defined url.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings("rawtypes")
public class CustomUrlStep extends AbstractProcessingStep {

    private final CustomUrlService customUrlService = new DSpace().getSingletonService(CustomUrlService.class);

    @Override
    @SuppressWarnings("unchecked")
    public CustomUrl getData(SubmissionService submissionService, InProgressSubmission obj,
                             SubmissionStepConfig config) throws Exception {

        Item item = obj.getItem();

        CustomUrl customUrl = new CustomUrl();
        customUrlService.getCustomUrl(item).ifPresent(customUrl::setUrl);
        customUrl.setRedirectedUrls(customUrlService.getOldCustomUrls(item));

        return customUrl;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
                                  Operation op, SubmissionStepConfig stepConf) throws Exception {

        String path = op.getPath();
        String stepId = stepConf.getId();

        PatchOperation<?> patchOperation = calculatePatchOperation(op, stepId)
            .orElseThrow(() -> new UnprocessableEntityException("Path " + path + " not supported by step " + stepId));

        patchOperation.perform(context, currentRequest, source, op);

    }

    private Optional<PatchOperation<?>> calculatePatchOperation(Operation operation, String stepId) {

        PatchOperationFactory patchOperationFactory = new PatchOperationFactory();
        String operationName = operation.getOp();

        if (operation.getPath().contains("/" + CUSTOM_URL_STEP_URL_OPERATION_ENTRY)) {
            return of(patchOperationFactory.instanceOf(CUSTOM_URL_STEP_URL_OPERATION_ENTRY, operationName));
        }

        if (operation.getPath().contains("/" + CUSTOM_URL_STEP_REDIRECTED_URL_OPERATION_ENTRY)) {
            return of(patchOperationFactory.instanceOf(CUSTOM_URL_STEP_REDIRECTED_URL_OPERATION_ENTRY, operationName));
        }

        if (isRemoveOperation(operation) && isWholeSectionPath(operation, stepId)) {
            return of(patchOperationFactory.instanceOf(CUSTOM_URL_STEP_URL_OPERATION_ENTRY, operationName));
        }

        return Optional.empty();
    }

    private boolean isWholeSectionPath(Operation operation, String stepId) {
        String operationPath = operation.getPath();
        return operationPath.endsWith(stepId + "/") || operationPath.endsWith(stepId);
    }

    private boolean isRemoveOperation(Operation operation) {
        return OPERATION_REMOVE.equalsIgnoreCase(operation.getOp());
    }


}
