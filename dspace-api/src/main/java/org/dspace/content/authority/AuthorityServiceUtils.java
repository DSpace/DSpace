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
 * Collection of utility methods used by the *AuthorityServiceImpl.
 *
 * @author Andrea Bollini
 */
public final class AuthorityServiceUtils {
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(AuthorityServiceUtils.class);

    @Autowired(required = true)
    protected UploadConfigurationService uploadConfigurationService;

    /**
     * Resolves the configuration name (Submission Name or Form Name) used as a key
     * for authority lookups, based on the DSpace Object type and Collection context.
     *
     * @param configReader the Submission Config Reader to query for configurations
     * @param dsoType      the type of DSpace Object (ITEM or BITSTREAM). Other types
     *                     return null.
     * @param collection   the collection context used to determine the assigned
     *                     submission process
     * @return the name of the submission process or metadata form, or null if no
     *         mapping is found or the type is unsupported
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
     * Resolves the configuration name used as a key for authority lookups using
     * the service layer instead of the reader.
     *
     * @param submissionConfigService the service used to access submission configurations
     * @param dsoType                 the type of DSpace Object (ITEM or BITSTREAM)
     * @param collection              the collection context
     * @return the name of the submission process or metadata form, or null if no
     *         mapping is found
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
