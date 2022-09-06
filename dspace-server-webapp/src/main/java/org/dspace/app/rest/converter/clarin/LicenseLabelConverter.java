package org.dspace.app.rest.converter.clarin;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.LicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseLabelConverter implements DSpaceConverter<LicenseLabel, ClarinLicenseLabelRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public ClarinLicenseLabelRest convert(LicenseLabel modelObject, Projection projection) {
        ClarinLicenseLabelRest licenseLabel = new ClarinLicenseLabelRest();
        licenseLabel.setProjection(projection);
        licenseLabel.setId(modelObject.getId());
        licenseLabel.setTitle(modelObject.getTitle());
        licenseLabel.setDefinition(modelObject.getDefinition());
        licenseLabel.setExtended(modelObject.isExtended());
        return licenseLabel;
    }

    @Override
    public Class<LicenseLabel> getModelClass() {
        return LicenseLabel.class;
    }
}
