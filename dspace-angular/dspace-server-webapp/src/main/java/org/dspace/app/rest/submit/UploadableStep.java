/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.IOException;

import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.springframework.web.multipart.MultipartFile;

/**
 * The interface for submission Steps that need to deal with file upload
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface UploadableStep extends ListenerProcessingStep {

    /**
     * The method to implement to support upload of a file in the submission section (aka panel / step)
     * 
     * @param context
     *            the dspace context
     * @param submissionService
     *            the submission service
     * @param stepConfig
     *            the configuration of the submission section
     * @param wsi
     *            the inprogress submission
     * @param file
     *            the multipart file, please note that it is a complex object containing additional information other
     *            than just the binary such as the filename and the mimetype
     * @return the encountered error if any
     * @throws IOException
     */
    public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
                            InProgressSubmission wsi, MultipartFile file) throws IOException;

}
