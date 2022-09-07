package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Component(ClarinLicenseRest.CATEGORY + "." + ClarinLicenseRest.NAME)
public class ClarinLicenseRestRepository extends DSpaceRestRepository<ClarinLicenseRest, Integer> {

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinLicenseRest findOne(Context context, Integer idValue) {
        ClarinLicense clarinLicense = null;
        try {
            clarinLicense = clarinLicenseService.find(context, idValue);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (Objects.isNull(clarinLicense)) {
            return null;
        }
        return converter.toRest(clarinLicense, utils.obtainProjection());
    }

    @Override
    public Page<ClarinLicenseRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinLicense> clarinLicenseList = clarinLicenseService.findAll(context);
            return converter.toRestPage(clarinLicenseList, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ClarinLicenseRest> getDomainClass() {
        return ClarinLicenseRest.class;
    }

}
