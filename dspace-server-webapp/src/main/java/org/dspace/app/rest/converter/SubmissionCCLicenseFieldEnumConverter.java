/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SubmissionCCLicenseFieldEnumRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.license.CCLicenseFieldEnum;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an CCLicenseFieldEnum to the REST
 * representation of an CCLicenseFieldEnum and vice versa
 * The CCLicenseFieldEnum is a sub component of the CCLicenseField object
 **/
@Component
public class SubmissionCCLicenseFieldEnumConverter
        implements DSpaceConverter<CCLicenseFieldEnum, SubmissionCCLicenseFieldEnumRest> {

    /**
     * Convert a CCLicenseFieldEnum to its REST representation
     *
     * @param modelObject - the CCLicenseField to convert
     * @param projection  - the projection
     * @return the corresponding SubmissionCCLicenseFieldEnumRest object
     */
    @Override
    public SubmissionCCLicenseFieldEnumRest convert(final CCLicenseFieldEnum modelObject, final Projection projection) {
        SubmissionCCLicenseFieldEnumRest submissionCCLicenseFieldEnumRest = new SubmissionCCLicenseFieldEnumRest();
        submissionCCLicenseFieldEnumRest.setId(modelObject.getId());
        submissionCCLicenseFieldEnumRest.setLabel(modelObject.getLabel());
        submissionCCLicenseFieldEnumRest.setDescription(modelObject.getDescription());

        return submissionCCLicenseFieldEnumRest;
    }

    @Override
    public Class<CCLicenseFieldEnum> getModelClass() {
        return CCLicenseFieldEnum.class;
    }

}
