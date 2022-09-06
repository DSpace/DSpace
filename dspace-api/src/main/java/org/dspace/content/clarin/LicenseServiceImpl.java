package org.dspace.content.clarin;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.content.dao.clarin.LicenseDAO;
import org.dspace.content.service.clarin.LicenseService;
import org.dspace.core.Context;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class LicenseServiceImpl implements LicenseService {

    @Autowired
    LicenseDAO licenseDAO;

    @Override
    public License create(Context context, License license) throws SQLException {
        return licenseDAO.create(context, license);
    }

    @Override
    public License find(Context context, int valueId) throws SQLException {
        return licenseDAO.findByID(context, License.class, valueId);
    }

    @Override
    public List<License> findAll(Context context) throws SQLException {
        return licenseDAO.findAll(context, License.class);
    }


    @Override
    public void delete(Context context, License license) throws SQLException {
        licenseDAO.delete(context, license);
    }

    @Override
    public void update(Context context, License oldLicense, License newLicense) throws SQLException {
        if (Objects.isNull(oldLicense) || Objects.isNull(newLicense)) {
            throw new NullArgumentException("Cannot update license because old or new license is null");
        }

        License foundLicense = find(context, oldLicense.getId());
        if (Objects.isNull(foundLicense)) {
            throw new ObjectNotFoundException(oldLicense.getId(), "Cannot update the license because the old license wasn't found " +
                    "in the database.");
        }

        licenseDAO.save(context, newLicense);
    }
}
