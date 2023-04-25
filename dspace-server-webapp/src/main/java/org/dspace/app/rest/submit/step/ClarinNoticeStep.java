/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * Clarin Notice step for DSpace Spring Rest. This step will show addition information about the current collection
 * where the item is creating.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinNoticeStep extends AbstractProcessingStep {

    @Override
    public <T extends Serializable> T getData(SubmissionService submissionService, InProgressSubmission obj,
                                              SubmissionStepConfig config) throws Exception {
        return null;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
    }
}
