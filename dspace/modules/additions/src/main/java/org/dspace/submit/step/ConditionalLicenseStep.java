/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;

/**
 * Custom Implementation for LIBDRUM-581
 * 
 * License step for DSpace Submission Process. Processes the
 * user response to the license.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author Mohamed Abdul Rasheed
 * @version $Revision$
 */
public class ConditionalLicenseStep extends LicenseStep
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ConditionalLicenseStep.class);

    /**
     * Returns true if there are no uploaded bitstreams.
     * 
     * @param @submissionInfo
     *              SubmissionInfo object
     */
    @Override
    public boolean canSkip(SubmissionInfo submissionInfo) throws SQLException {
        Item item = submissionInfo.getSubmissionItem().getItem();
        java.util.List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
        java.util.List<Bitstream> bitstreams = new java.util.ArrayList<>();
        if (bundles.size() > 0)
        {
            bitstreams = bundles.get(0).getBitstreams();
        }
        boolean noUploadedFiles = bitstreams.size() < 1;
        log.info("noUploadedFiles: " + noUploadedFiles + " bitstreams.size(): " + bitstreams.size());
        return noUploadedFiles;
    }
}
