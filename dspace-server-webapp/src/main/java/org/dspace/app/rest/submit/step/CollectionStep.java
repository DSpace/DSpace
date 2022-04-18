/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.submit.DataProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * Collection step for DSpace Spring Rest. Expose the collection information of
 * the in progress submission.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class CollectionStep implements DataProcessingStep {

    @Override
    public UUID getData(SubmissionService submissionService, InProgressSubmission obj, SubmissionStepConfig config) {
        if (obj.getCollection() != null) {
            return obj.getCollection().getID();
        }
        return null;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
        PatchOperation<String> patchOperation = new PatchOperationFactory()
            .instanceOf(COLLECTION_STEP_OPERATION_ENTRY, op.getOp());
        patchOperation.perform(context, currentRequest, source, op);
    }
}
