/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SubmissionCCLicenseUrlRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;


/**
 * This converter is responsible for transforming a Submission CC License Url String to the REST
 * representation SubmissionCCLicenseUrlRest and vice versa
 */
@Component
public class SubmissionCCLicenseUrlConverter implements DSpaceConverter<String, SubmissionCCLicenseUrlRest> {

    /**
     * Convert a Submission CC License Url String to its REST representation
     * @param modelObject   - the CC License Url String to convert
     * @param projection    - the projection
     * @return the corresponding SubmissionCCLicenseUrlRest object
     */
    @Override
    public SubmissionCCLicenseUrlRest convert(final String modelObject, final Projection projection) {
        SubmissionCCLicenseUrlRest submissionCCLicenseUrlRest = new SubmissionCCLicenseUrlRest();
        submissionCCLicenseUrlRest.setUrl(modelObject);
        submissionCCLicenseUrlRest.setId(modelObject);

        return submissionCCLicenseUrlRest;
    }

    @Override
    public Class<String> getModelClass() {
        return String.class;
    }

}
