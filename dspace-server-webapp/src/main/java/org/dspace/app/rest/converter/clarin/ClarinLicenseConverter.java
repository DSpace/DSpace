package org.dspace.app.rest.converter.clarin;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.converter.MetadataValueDTOConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelList;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClarinLicenseConverter implements DSpaceConverter<ClarinLicense, ClarinLicenseRest> {

    @Autowired
    private ConverterService converter;

    @Autowired
    private ClarinLicenseLabelListConverter clarinLicenseLabelListConverter;


    @Override
    public ClarinLicenseRest convert(ClarinLicense modelObject, Projection projection) {
        ClarinLicenseRest license = new ClarinLicenseRest();
        license.setProjection(projection);
        license.setId(modelObject.getId());
        license.setConfirmation(modelObject.getConfirmation());
        license.setDefinition(modelObject.getDefinition());
        license.setRequiredInfo(modelObject.getRequiredInfo());
        license.setClarinLicenseLabels(modelObject.getLicenseLabels());
        return license;
    }

    @Override
    public Class<ClarinLicense> getModelClass() {
        return ClarinLicense.class;
    }
}
