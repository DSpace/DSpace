/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.SubmissionCCLicenseFieldRest;
import org.dspace.app.rest.model.SubmissionCCLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.license.CCLicense;
import org.dspace.license.CCLicenseField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an CCLicense to the REST
 * representation of an CCLicense and vice versa
 **/
@Component
public class SubmissionCCLicenseConverter implements DSpaceConverter<CCLicense, SubmissionCCLicenseRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    /**
     * Convert a CCLicense to its REST representation
     * @param modelObject   - the CCLicense to convert
     * @param projection    - the projection
     * @return the corresponding SubmissionCCLicenseRest object
     */
    @Override
    public SubmissionCCLicenseRest convert(final CCLicense modelObject, final Projection projection) {
        SubmissionCCLicenseRest submissionCCLicenseRest = new SubmissionCCLicenseRest();
        submissionCCLicenseRest.setProjection(projection);
        submissionCCLicenseRest.setId(modelObject.getLicenseId());
        submissionCCLicenseRest.setName(modelObject.getLicenseName());

        List<CCLicenseField> ccLicenseFieldList = modelObject.getCcLicenseFieldList();
        List<SubmissionCCLicenseFieldRest> submissionCCLicenseFieldRests = new LinkedList<>();
        if (ccLicenseFieldList != null) {
            for (CCLicenseField ccLicenseField : ccLicenseFieldList) {
                submissionCCLicenseFieldRests.add(converter.toRest(ccLicenseField, projection));
            }
        }
        submissionCCLicenseRest.setFields(submissionCCLicenseFieldRests);
        return submissionCCLicenseRest;
    }

    @Override
    public Class<CCLicense> getModelClass() {
        return CCLicense.class;
    }

}
