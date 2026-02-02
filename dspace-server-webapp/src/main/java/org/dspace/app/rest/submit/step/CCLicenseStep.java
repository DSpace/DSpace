/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataCCLicense;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * CC License step for DSpace Spring Rest. Expose the creative commons license information about the in progress
 * submission.
 */
public class CCLicenseStep extends AbstractProcessingStep {

    /**
     * Retrieves the CC License data of the in progress submission
     *
     * @param submissionService the submission service
     * @param obj               the in progress submission
     * @param config            the submission step configuration
     * @return the CC License data of the in progress submission
     * @throws Exception
     */
    @Override
    public DataCCLicense getData(SubmissionService submissionService, InProgressSubmission obj,
                                 SubmissionStepConfig config)
            throws Exception {
        return submissionService.getDataCCLicense(obj);
    }


    /**
     * Processes a patch for the CC License data
     *
     * @param context        the DSpace context
     * @param currentRequest the http request
     * @param source         the in progress submission
     * @param op             the json patch operation
     * @throws Exception
     */
    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {

        if (op.getPath().endsWith(CCLICENSE_STEP_OPERATION_ENTRY)) {

            PatchOperation<String> patchOperation = new PatchOperationFactory()
                    .instanceOf(CCLICENSE_STEP_OPERATION_ENTRY, op.getOp());
            patchOperation.perform(context, currentRequest, source, op);

        }
    }
}
