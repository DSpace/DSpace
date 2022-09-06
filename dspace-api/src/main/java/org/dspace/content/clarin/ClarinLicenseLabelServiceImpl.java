package org.dspace.content.clarin;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.content.dao.clarin.ClarinLicenseLabelDAO;
import org.dspace.content.service.clarin.LicenseLabelService;
import org.dspace.core.Context;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Objects;

public class ClarinLicenseLabelServiceImpl implements LicenseLabelService {

    @Autowired
    ClarinLicenseLabelDAO clarinLicenseLabelDAO;

    @Override
    public ClarinLicenseLabel create(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException {
        return clarinLicenseLabelDAO.create(context, clarinLicenseLabel);
    }

    @Override
    public ClarinLicenseLabel find(Context context, int valueId) throws SQLException {
        return clarinLicenseLabelDAO.findByID(context, ClarinLicenseLabel.class, valueId);
    }

    @Override
    public void delete(Context context, ClarinLicenseLabel license) throws SQLException {
        clarinLicenseLabelDAO.delete(context, license);
    }

    @Override
    public void update(Context context, ClarinLicenseLabel oldClarinLicenseLabel, ClarinLicenseLabel newClarinLicenseLabel) throws SQLException {
        if (Objects.isNull(oldClarinLicenseLabel) || Objects.isNull(newClarinLicenseLabel)) {
            throw new NullArgumentException("Cannot update licenseLabel because old or new licenseLabel is null");
        }

        ClarinLicenseLabel foundClarinLicenseLabel = find(context, oldClarinLicenseLabel.getId());
        if (Objects.isNull(foundClarinLicenseLabel)) {
            throw new ObjectNotFoundException(oldClarinLicenseLabel.getId(), "Cannot update the licenseLabel because " +
                    "the old licenseLabel wasn't found in the database.");
        }

        clarinLicenseLabelDAO.save(context, newClarinLicenseLabel);
    }
}
