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

import org.dspace.app.rest.model.SubmissionCCLicenseFieldEnumRest;
import org.dspace.app.rest.model.SubmissionCCLicenseFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.license.CCLicenseField;
import org.dspace.license.CCLicenseFieldEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an CCLicenseField to the REST
 * representation of an CCLicenseField and vice versa
 * The CCLicenseField is a sub component of the CCLicense object
 **/
@Component
public class SubmissionCCLicenseFieldConverter
        implements DSpaceConverter<CCLicenseField, SubmissionCCLicenseFieldRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    /**
     * Convert a CCLicenseField to its REST representation
     * @param modelObject   - the CCLicenseField to convert
     * @param projection    - the projection
     * @return the corresponding SubmissionCCLicenseFieldRest object
     */
    @Override
    public SubmissionCCLicenseFieldRest convert(final CCLicenseField modelObject, final Projection projection) {
        SubmissionCCLicenseFieldRest submissionCCLicenseFieldRest = new SubmissionCCLicenseFieldRest();
        submissionCCLicenseFieldRest.setId(modelObject.getId());
        submissionCCLicenseFieldRest.setLabel(modelObject.getLabel());
        submissionCCLicenseFieldRest.setDescription(modelObject.getDescription());

        List<CCLicenseFieldEnum> fieldEnum = modelObject.getFieldEnum();
        List<SubmissionCCLicenseFieldEnumRest> submissionCCLicenseFieldEnumRests = new LinkedList<>();
        if (fieldEnum != null) {
            for (CCLicenseFieldEnum ccLicenseFieldEnum : fieldEnum) {
                submissionCCLicenseFieldEnumRests.add(converter.toRest(ccLicenseFieldEnum, projection));
            }
        }
        submissionCCLicenseFieldRest.setEnums(submissionCCLicenseFieldEnumRests);
        return submissionCCLicenseFieldRest;
    }

    @Override
    public Class<CCLicenseField> getModelClass() {
        return CCLicenseField.class;
    }

}
