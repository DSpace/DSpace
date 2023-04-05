/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SubmissionCCLicenseUrlRest;
import org.dspace.app.rest.model.wrapper.SubmissionCCLicenseUrl;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;


/**
 * This converter is responsible for transforming a Submission CC License Url String to the REST
 * representation SubmissionCCLicenseUrlRest and vice versa
 */
@Component
public class SubmissionCCLicenseUrlConverter
    implements DSpaceConverter<SubmissionCCLicenseUrl, SubmissionCCLicenseUrlRest> {

    /**
     * Convert a Submission CC License Url String to its REST representation
     * @param modelObject   - the CC License Url object to convert
     * @param projection    - the projection
     * @return the corresponding SubmissionCCLicenseUrlRest object
     */
    @Override
    public SubmissionCCLicenseUrlRest convert(SubmissionCCLicenseUrl modelObject, Projection projection) {
        SubmissionCCLicenseUrlRest submissionCCLicenseUrlRest = new SubmissionCCLicenseUrlRest();
        submissionCCLicenseUrlRest.setUrl(modelObject.getUrl());
        submissionCCLicenseUrlRest.setId(modelObject.getId());

        return submissionCCLicenseUrlRest;
    }

    @Override
    public Class<SubmissionCCLicenseUrl> getModelClass() {
        return SubmissionCCLicenseUrl.class;
    }

}
