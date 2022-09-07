package org.dspace.content.clarin;

import org.apache.commons.lang.NullArgumentException;
import org.checkerframework.checker.units.qual.C;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.clarin.ClarinLicenseDAO;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupServiceImpl;
import org.dspace.event.Event;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ClarinLicenseServiceImpl implements ClarinLicenseService {

    private static final Logger log = LoggerFactory.getLogger(ClarinLicenseServiceImpl.class);

    @Autowired
    ClarinLicenseDAO clarinLicenseDAO;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public ClarinLicense create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an Clarin License");
        }

        // Create a table row
        ClarinLicense clarinLicense = clarinLicenseDAO.create(context, new ClarinLicense());

        log.info(LogHelper.getHeader(context, "create_clarin_license", "clarin_license_id="
                + clarinLicense.getID()));

        return clarinLicense;
    }

    @Override
    public ClarinLicense create(Context context, ClarinLicense clarinLicense) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an Clarin License");
        }

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
    public void update(Context context, ClarinLicense newClarinLicense) throws SQLException {
        if (Objects.isNull(newClarinLicense)) {
            throw new NullArgumentException("Cannot update clarin license because the new clarin license is null");
        }

        ClarinLicense foundClarinLicense = find(context, newClarinLicense.getId());
        if (Objects.isNull(foundClarinLicense)) {
            throw new ObjectNotFoundException(newClarinLicense.getId(), "Cannot update the license because the clarin license wasn't found " +
                    "in the database.");
        }

        clarinLicenseDAO.save(context, newClarinLicense);
    }
}
