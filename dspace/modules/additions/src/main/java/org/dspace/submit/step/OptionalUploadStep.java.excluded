/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import org.apache.log4j.Logger;

/**
 * // Custom implementation for LIBDRUM-581
 * 
 * Optional Upload step for DSpace. Processes the actual upload of files
 * for an item being submitted into DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Mohamed Abdul Rasheed
 */
public class OptionalUploadStep extends UploadStep
{

    /** log4j logger */
    private static final Logger log = Logger.getLogger(OptionalUploadStep.class);

    public OptionalUploadStep() {
        this.fileRequired = false;
    }
}
