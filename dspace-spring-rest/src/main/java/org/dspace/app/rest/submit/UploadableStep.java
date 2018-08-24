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
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public interface UploadableStep extends ListenerProcessingStep {

    public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
                            InProgressSubmission wsi, MultipartFile file, String extraField) throws IOException;

}
