/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataDuplicateDetection;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission processing step to detect potential duplicates of this item and list them so that
 * the submitter can choose to cancel or continue with their submission
 *
 * @author Kim Shepherd
 */
public class DuplicateDetectionStep extends AbstractProcessingStep {

    private static final Logger log = LogManager.getLogger(DuplicateDetectionStep.class);

    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    /**
     * Override DataProcessing.getData, return a list of potential duplicates
     *
     * @param submissionService The submission service
     * @param obj               The workspace or workflow item
     * @param config            The submission step configuration
     * @return                  A simple DataIdentifiers bean containing doi, handle and list of other identifiers
     */
    @Override
    public DataDuplicateDetection getData(SubmissionService submissionService, InProgressSubmission obj,
                                          SubmissionStepConfig config) throws Exception {
        // Validate in progress submission object and wrapped item
        if (obj == null) {
            throw new IllegalArgumentException("Null in-progress wrapper object");
        }
        if (obj.getItem() == null) {
            throw new IllegalArgumentException("Null in-progress item");
        }
        // Immediately return an empty if this feature is not configured
        if (!configurationService.getBooleanProperty("duplicate.enable", false)) {
            log.debug("Duplicate detection is not enabled, returning empty section");
            return new DataDuplicateDetection();
        }
        // Validate context
        Context context = getContext();
        if (context == null) {
            throw new ServletException("Null context");
        }

        // Return the constructed data section
        return submissionService.getDataDuplicateDetection(context, obj);
    }

    /**
     * Utility method to get DSpace context from the HTTP request
     * @return  DSpace context
     */
    private Context getContext() {
        Context context;
        Request currentRequest = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }

        return context;
    }

    /**
     * This step is currently just for displaying identifiers and does not take additional patch operations
     * @param context
     *            the DSpace context
     * @param currentRequest
     *            the http request
     * @param source
     *            the in progress submission
     * @param op
     *            the json patch operation
     * @param stepConf
     * @throws Exception
     */
    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
                                  Operation op, SubmissionStepConfig stepConf) throws Exception {
        log.warn("Not implemented");
    }

}
