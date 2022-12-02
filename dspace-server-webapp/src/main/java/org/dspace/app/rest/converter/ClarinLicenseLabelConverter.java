/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Clarin License Label in the DSpace API data model and the
 * REST data model
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class ClarinLicenseLabelConverter implements DSpaceConverter<ClarinLicenseLabel, ClarinLicenseLabelRest> {

    @Override
    public ClarinLicenseLabelRest convert(ClarinLicenseLabel modelObject, Projection projection) {
        ClarinLicenseLabelRest licenseLabel = new ClarinLicenseLabelRest();
        licenseLabel.setProjection(projection);
        licenseLabel.setId(modelObject.getID());
        licenseLabel.setTitle(modelObject.getTitle());
        licenseLabel.setLabel(modelObject.getLabel());
        licenseLabel.setExtended(modelObject.isExtended());
        licenseLabel.setIcon(modelObject.getIcon());
        return licenseLabel;
    }

    @Override
    public Class<ClarinLicenseLabel> getModelClass() {
        return ClarinLicenseLabel.class;
    }
}
