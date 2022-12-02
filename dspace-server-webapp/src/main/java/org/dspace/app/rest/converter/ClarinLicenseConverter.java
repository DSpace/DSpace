/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Clarin License in the DSpace API data model and the
 * REST data model
 * Clarin License Rest object has clarin license labels separated to the list of the extended clarin license labels
 * and one non-extended clarin license label
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinLicenseConverter implements DSpaceConverter<ClarinLicense, ClarinLicenseRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public ClarinLicenseRest convert(ClarinLicense modelObject, Projection projection) {
        ClarinLicenseRest license = new ClarinLicenseRest();
        license.setProjection(projection);
        license.setId(modelObject.getID());
        license.setName(modelObject.getName());
        license.setConfirmation(modelObject.getConfirmation());
        license.setDefinition(modelObject.getDefinition());
        license.setRequiredInfo(modelObject.getRequiredInfo());
        setExtendedClarinLicenseLabels(license, modelObject.getLicenseLabels(), projection);
        setClarinLicenseLabel(license, modelObject.getLicenseLabels(), projection);
        license.setBitstreams(modelObject.getNonDeletedBitstreams());
        return license;
    }

    @Override
    public Class<ClarinLicense> getModelClass() {
        return ClarinLicense.class;
    }


    /**
     * Clarin license labels separate to the list of the extended clarin license labels
     */
    public void setExtendedClarinLicenseLabels(ClarinLicenseRest licenseRest, List<ClarinLicenseLabel> cLicenseLabels,
                                               Projection projection) {
        DSpaceConverter<ClarinLicenseLabel, ClarinLicenseLabelRest> clarinLicenseLabelConverter =
                converter.getConverter(ClarinLicenseLabel.class);

        List<ClarinLicenseLabelRest> clarinLicenseLabelRestList = new ArrayList<>();
        for (ClarinLicenseLabel clarinLicenseLabel : cLicenseLabels) {
            if (!clarinLicenseLabel.isExtended()) {
                continue;
            }
            licenseRest.getExtendedClarinLicenseLabels().add(clarinLicenseLabelConverter.convert(clarinLicenseLabel,
                    projection));
        }
    }

    /**
     * Get non-extended clarin license label from clarin license labels
     */
    public void setClarinLicenseLabel(ClarinLicenseRest licenseRest, List<ClarinLicenseLabel> cLicenseLabels,
                                      Projection projection) {
        DSpaceConverter<ClarinLicenseLabel, ClarinLicenseLabelRest> clarinLicenseLabelConverter =
                converter.getConverter(ClarinLicenseLabel.class);
        for (ClarinLicenseLabel clarinLicenseLabel : cLicenseLabels) {
            if (clarinLicenseLabel.isExtended()) {
                continue;
            }

            licenseRest.setClarinLicenseLabel(clarinLicenseLabelConverter.convert(clarinLicenseLabel, projection));
            // there is only one non-extended license label
            return;
        }
    }
}
