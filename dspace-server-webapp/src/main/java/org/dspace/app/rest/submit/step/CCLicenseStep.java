/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataCCLicense;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.services.model.Request;


public class CCLicenseStep extends org.dspace.submit.step.CCLicenseStep implements AbstractRestProcessingStep {

    @Override
    public DataCCLicense getData(SubmissionService submissionService, InProgressSubmission obj,
                                 SubmissionStepConfig config)
            throws Exception {
        return submissionService.getDataCCLicense(obj);
    }


    @Override
    public void doPatchProcessing(Context context, Request currentRequest, InProgressSubmission source, Operation op)
            throws Exception {

        if (op.getPath().endsWith(CCLICENSE_STEP_OPERATION_ENTRY)) {

            PatchOperation<String> patchOperation = new PatchOperationFactory()
                    .instanceOf(CCLICENSE_STEP_OPERATION_ENTRY, op.getOp());
            patchOperation.perform(context, currentRequest, source, op);

        }
    }
}
