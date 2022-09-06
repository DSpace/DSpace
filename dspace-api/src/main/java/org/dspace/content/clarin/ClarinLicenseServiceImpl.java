package org.dspace.content.clarin;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.content.dao.clarin.ClarinLicenseDAO;
import org.dspace.content.service.clarin.LicenseService;
import org.dspace.core.Context;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ClarinLicenseServiceImpl implements LicenseService {

    @Autowired
    ClarinLicenseDAO clarinLicenseDAO;

    @Override
    public ClarinLicense create(Context context, ClarinLicense clarinLicense) throws SQLException {
        return clarinLicenseDAO.create(context, clarinLicense);
    }

    @Override
    public ClarinLicense find(Context context, int valueId) throws SQLException {
        return clarinLicenseDAO.findByID(context, ClarinLicense.class, valueId);
    }

    @Override
    public List<ClarinLicense> findAll(Context context) throws SQLException {
        return clarinLicenseDAO.findAll(context, ClarinLicense.class);
    }


    @Override
    public void delete(Context context, ClarinLicense clarinLicense) throws SQLException {
        clarinLicenseDAO.delete(context, clarinLicense);
    }

    @Override
    public void update(Context context, ClarinLicense oldClarinLicense, ClarinLicense newClarinLicense) throws SQLException {
        if (Objects.isNull(oldClarinLicense) || Objects.isNull(newClarinLicense)) {
            throw new NullArgumentException("Cannot update license because old or new license is null");
        }

        ClarinLicense foundClarinLicense = find(context, oldClarinLicense.getId());
        if (Objects.isNull(foundClarinLicense)) {
            throw new ObjectNotFoundException(oldClarinLicense.getId(), "Cannot update the license because the old license wasn't found " +
                    "in the database.");
        }

        clarinLicenseDAO.save(context, newClarinLicense);
    }
}
