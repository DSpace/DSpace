package org.dspace.app.rest.converter;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClarinLicenseLabelConverter implements DSpaceConverter<ClarinLicenseLabel, ClarinLicenseLabelRest> {

    @Override
    public ClarinLicenseLabelRest convert(ClarinLicenseLabel modelObject, Projection projection) {
        ClarinLicenseLabelRest licenseLabel = new ClarinLicenseLabelRest();
        licenseLabel.setProjection(projection);
        licenseLabel.setId(modelObject.getId());
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
