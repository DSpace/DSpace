package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(ClarinLicenseLabelRest.CATEGORY + "." + ClarinLicenseLabelRest.NAME)
public class LicenseLabelRestRepository extends DSpaceRestRepository<ClarinLicenseLabelRest, Integer> {
    @Override
    public ClarinLicenseLabelRest findOne(Context context, Integer integer) {
        return null;
    }

    @Override
    public Page<ClarinLicenseLabelRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    @Override
    public Class<ClarinLicenseLabelRest> getDomainClass() {
        return ClarinLicenseLabelRest.class;
    }
}
