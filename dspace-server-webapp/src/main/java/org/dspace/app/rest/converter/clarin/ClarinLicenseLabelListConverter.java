package org.dspace.app.rest.converter.clarin;

import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelList;
import org.dspace.app.rest.model.ClarinLicenseLabelListRest;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClarinLicenseLabelListConverter implements DSpaceConverter<ClarinLicenseLabelList, ClarinLicenseLabelListRest> {
//    @Override
//    public ClarinLicenseLabelListRest convert(ClarinLicenseLabelList clarinLicenseLabelList) {
//        ClarinLicenseLabelListRest clarinLicenseLabelListRest = new ClarinLicenseLabelListRest();
//        for (ClarinLicenseLabel clarinLicenseLabel : clarinLicenseLabelList.getList()) {
//            ClarinLicenseLabelRest clarinLicenseLabelRest = new ClarinLicenseLabelRest();
//            clarinLicenseLabelRest.setDefinition(clarinLicenseLabel.getDefinition());
//            clarinLicenseLabelRest.setExtended(clarinLicenseLabel.isExtended());
//            clarinLicenseLabelRest.setTitle(clarinLicenseLabel.getTitle());
//            clarinLicenseLabelListRest.add(clarinLicenseLabelRest);
//        }
//
//        return clarinLicenseLabelListRest;
//    }

    @Override
    public ClarinLicenseLabelListRest convert(ClarinLicenseLabelList modelObject, Projection projection) {
        ClarinLicenseLabelListRest clarinLicenseLabelListRest = new ClarinLicenseLabelListRest();
        List<ClarinLicenseLabelRest> clarinLicenseLabelListRestList = new ArrayList<>();
        for (ClarinLicenseLabel clarinLicenseLabel : modelObject.getList()) {
            ClarinLicenseLabelRest clarinLicenseLabelRest = new ClarinLicenseLabelRest();
            clarinLicenseLabelRest.setLabel(clarinLicenseLabel.getLabel());
            clarinLicenseLabelRest.setExtended(clarinLicenseLabel.isExtended());
            clarinLicenseLabelRest.setTitle(clarinLicenseLabel.getTitle());
//            clarinLicenseLabelListRest.put(clarinLicenseLabel.getTitle(), clarinLicenseLabelRest);
//            clarinLicenseLabelListRest.add(clarinLicenseLabelRest);
            clarinLicenseLabelListRestList.add(clarinLicenseLabelRest);
        }
//        clarinLicenseLabelListRest.add(clarinLicenseLabelListRestList);

        return clarinLicenseLabelListRest;
    }

    @Override
    public Class<ClarinLicenseLabelList> getModelClass() {
        return ClarinLicenseLabelList.class;
    }
}
