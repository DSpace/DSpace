/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.openpolicyfinder.cache.OpenPolicyFinderCacheEvictService;
import org.dspace.app.openpolicyfinder.submit.OpenPolicyFinderSubmitService;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.OpenPolicyFinderPolicy;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.web.ContextUtil;

/**
 * OpenPolicyFinderPolicy step for DSpace Spring Rest. Expose information about
 * the Open Policy Finder policies for the in progress submission.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenPolicyFinderPolicyStep extends AbstractProcessingStep {

    public static final String RETRIEVAL_TIME = "retrievalTime";

    private OpenPolicyFinderCacheEvictService opfCacheEvictService = new DSpace().getSingletonService(
                OpenPolicyFinderCacheEvictService.class);
    private OpenPolicyFinderSubmitService opfSubmitService =
                new DSpace().getSingletonService(OpenPolicyFinderSubmitService.class);

    @Override
    @SuppressWarnings("unchecked")
    public OpenPolicyFinderPolicy getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        OpenPolicyFinderResponse response = opfSubmitService.searchRelatedJournals(context, obj.getItem());
        if (Objects.nonNull(response)) {
            OpenPolicyFinderPolicy result = new OpenPolicyFinderPolicy();
            result.setOpfResponse(response);
            return result;
        }
        return null;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
        String path = op.getPath();
        if (path.contains(RETRIEVAL_TIME)) {
            opfCacheEvictService.evictCacheValues(context, source.getItem());
        }
    }

}