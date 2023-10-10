/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataCOARNotify;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * COARNotify Step for DSpace Spring Rest. Expose information about
 * the COAR Notify services for the in progress submission.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class COARNotifyStep extends AbstractProcessingStep {

    /**
     * Retrieves the COAR Notify services data of the in progress submission
     *
     * @param submissionService the submission service
     * @param obj               the in progress submission
     * @param config            the submission step configuration
     * @return the COAR Notify data of the in progress submission
     * @throws Exception
     */
    @Override
    public ArrayList<DataCOARNotify> getData(SubmissionService submissionService, InProgressSubmission obj,
                                             SubmissionStepConfig config) throws Exception {
        return submissionService.getDataCOARNotify(obj);
    }

    /**
     * Processes a patch for the COAR Notify data
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

    }
}
