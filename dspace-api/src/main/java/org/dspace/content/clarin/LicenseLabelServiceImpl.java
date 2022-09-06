package org.dspace.content.clarin;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.content.dao.clarin.LicenseLabelDAO;
import org.dspace.content.service.clarin.LicenseLabelService;
import org.dspace.core.Context;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Objects;

public class LicenseLabelServiceImpl implements LicenseLabelService {

    @Autowired
    LicenseLabelDAO licenseLabelDAO;

    @Override
    public LicenseLabel create(Context context, LicenseLabel licenseLabel) throws SQLException {
        return licenseLabelDAO.create(context, licenseLabel);
    }

    @Override
    public LicenseLabel find(Context context, int valueId) throws SQLException {
        return licenseLabelDAO.findByID(context, LicenseLabel.class, valueId);
    }

    @Override
    public void delete(Context context, LicenseLabel license) throws SQLException {
        licenseLabelDAO.delete(context, license);
    }

    @Override
    public void update(Context context, LicenseLabel oldLicenseLabel, LicenseLabel newLicenseLabel) throws SQLException {
        if (Objects.isNull(oldLicenseLabel) || Objects.isNull(newLicenseLabel)) {
            throw new NullArgumentException("Cannot update licenseLabel because old or new licenseLabel is null");
        }

        LicenseLabel foundLicenseLabel = find(context, oldLicenseLabel.getId());
        if (Objects.isNull(foundLicenseLabel)) {
            throw new ObjectNotFoundException(oldLicenseLabel.getId(), "Cannot update the licenseLabel because " +
                    "the old licenseLabel wasn't found in the database.");
        }

        licenseLabelDAO.save(context, newLicenseLabel);
    }
}
