/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.Serializable;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

/**
 * Interface for the submission steps to populate sections in the in progress submission and react to patch requests.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface AbstractRestProcessingStep extends ListenerProcessingStep {

    public static final String DESCRIBE_STEP_METADATA_OPERATION_ENTRY = "itemmetadata";
    public static final String COLLECTION_STEP_OPERATION_ENTRY = "collection";
    public static final String UPLOAD_STEP_METADATA_OPERATION_ENTRY = "bitstreammetadata";
    public static final String UPLOAD_STEP_REMOVE_OPERATION_ENTRY = "bitstreamremove";
    public static final String UPLOAD_STEP_MOVE_OPERATION_ENTRY = "bitstreammove";
    public static final String UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY = "accessConditions";
    public static final String LICENSE_STEP_OPERATION_ENTRY = "granted";
    public static final String CCLICENSE_STEP_OPERATION_ENTRY = "cclicense/uri";

    public static final String UPLOAD_STEP_METADATA_PATH = "metadata";

    /**
     * Method to expose data in the a dedicated section of the in progress submission. The step needs to return a
     * serializable object that will be included in a section with the name (id) assigned to the step in the
     * item-submission.xml file
     * 
     * @param submissionService
     *            the submission service
     * @param obj
     *            the in progress submission
     * @param config
     *            the submission step configuration
     * @return the serializable object to include in the step generated section
     * @throws Exception
     */
    public <T extends Serializable> T getData(SubmissionService submissionService, InProgressSubmission obj,
                                              SubmissionStepConfig config) throws Exception;

    /**
     * Method to react to a patch request against the step managed section data
     * 
     * @param context
     *            the DSpace context
     * @param currentRequest
     *            the http request
     * @param source
     *            the in progress submission
     * @param op
     *            the json patch operation
     * @throws Exception
     */
    public void doPatchProcessing(Context context, Request currentRequest, InProgressSubmission source, Operation op,
                                  SubmissionStepConfig stepConf) throws Exception;

}
