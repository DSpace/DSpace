package org.dspace.app.rest.converter.clarin;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.License;
import org.dspace.content.clarin.LicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LicenseConverter implements DSpaceConverter<License, ClarinLicenseRest> {

    @Autowired
    private ConverterService converter;


    @Override
    public ClarinLicenseRest convert(License modelObject, Projection projection) {
        ClarinLicenseRest license = new ClarinLicenseRest();
        license.setProjection(projection);
        license.setId(modelObject.getId());
        license.setConfirmation(modelObject.getConfirmation());
        license.setDefinition(modelObject.getDefinition());
        license.setRequiredInfo(modelObject.getRequiredInfo());
        List<LicenseLabel> lll = modelObject.getLicenseLabels();
        LicenseLabel ll = null;
        if (CollectionUtils.isEmpty(lll)) {
            ll = new LicenseLabel();
        } else {
            ll = lll.get(0);
        }

        ClarinLicenseLabelRest llr = converter.toRest(ll, projection);
        license.setLicenseLabel(llr);
        return license;
    }

    @Override
    public Class<License> getModelClass() {
        return License.class;
    }
}
