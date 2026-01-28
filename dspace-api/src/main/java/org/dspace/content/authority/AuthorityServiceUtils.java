/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.logging.log4j.Logger;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.submit.service.SubmissionConfigService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Collection of utility methods used by the *AuthorityServiceImpl
 *
 * @author Andrea Bollini
 */
public final class AuthorityServiceUtils {
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(AuthorityServiceUtils.class);

    @Autowired(required = true)
    protected UploadConfigurationService uploadConfigurationService;

    /**
     *
     * @param configReader the Submission Config Reader
     * @param dsoType      the type of dspace object (ITEM or BITSTREAM) for all the
     *                     other object <code>null</code> is returned
     * @param collection   the collection where the object stays
     * @return the name of the submission form (if ITEM) or the name of the metadata
     *         form (BITSTREAM)
     */
    public String getSubmissionOrFormName(SubmissionConfigReader configReader, int dsoType,
            Collection collection) {
        switch (dsoType) {
            case Constants.ITEM:
                return configReader.getSubmissionConfigByCollection(collection).getSubmissionName();
            case Constants.BITSTREAM:
                SubmissionConfig subCfg = configReader.getSubmissionConfigByCollection(collection);
                for (int i = 0; i < subCfg.getNumberOfSteps(); i++) {
                    SubmissionStepConfig step = subCfg.getStep(i);
                    if (SubmissionStepConfig.UPLOAD_STEP_NAME.equalsIgnoreCase(step.getType())) {
                        return uploadConfigurationService.getMap().get(step.getId()).getMetadata();
                    }
                }
                return null;
            default:
                return null;
        }
    }

    /**
     *
     * @param submissionConfigService the Submission Config service
     * @param dsoType      the type of dspace object (ITEM or BITSTREAM) for all the
     *                     other object <code>null</code> is returned
     * @param collection   the collection where the object stays
     * @return the name of the submission form (if ITEM) or the name of the metadata
     *         form (BITSTREAM)
     */
    public String getSubmissionOrFormName(SubmissionConfigService submissionConfigService, int dsoType,
                                          Collection collection) {
        switch (dsoType) {
            case Constants.ITEM:
                return submissionConfigService.getSubmissionConfigByCollection(collection).getSubmissionName();
            case Constants.BITSTREAM:
                SubmissionConfig subCfg = submissionConfigService.getSubmissionConfigByCollection(collection);
                for (int i = 0; i < subCfg.getNumberOfSteps(); i++) {
                    SubmissionStepConfig step = subCfg.getStep(i);
                    if (SubmissionStepConfig.UPLOAD_STEP_NAME.equalsIgnoreCase(step.getType())) {
                        return uploadConfigurationService.getMap().get(step.getId()).getMetadata();
                    }
                }
                return null;
            default:
                return null;
        }
    }
}
