/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.NullArgumentException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.clarin.ClarinLicenseLabelDAO;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Clarin License Label object.
 * This class is responsible for all business logic calls for the Clarin License Label object and is autowired
 * by spring.
 * This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseLabelServiceImpl implements ClarinLicenseLabelService {

    private static final Logger log = LoggerFactory.getLogger(ClarinLicenseServiceImpl.class);

    @Autowired
    ClarinLicenseLabelDAO clarinLicenseLabelDAO;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public ClarinLicenseLabel create(Context context) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License Label");
        }

        // Create a table row
        ClarinLicenseLabel clarinLicenseLabel = clarinLicenseLabelDAO.create(context, new ClarinLicenseLabel());
        log.info(LogHelper.getHeader(context, "create_clarin_license_label", "clarin_license_label_id="
                + clarinLicenseLabel.getID()));

        return clarinLicenseLabel;
    }

    @Override
    public ClarinLicenseLabel create(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException,
            AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License Label");
        }

        return clarinLicenseLabelDAO.create(context, clarinLicenseLabel);
    }

    @Override
    public ClarinLicenseLabel find(Context context, int valueId) throws SQLException {
        return clarinLicenseLabelDAO.findByID(context, ClarinLicenseLabel.class, valueId);
    }

    @Override
    public List<ClarinLicenseLabel> findAll(Context context) throws SQLException, AuthorizeException {
        return clarinLicenseLabelDAO.findAll(context, ClarinLicenseLabel.class);
    }

    @Override
    public void delete(Context context, ClarinLicenseLabel license) throws SQLException, AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License Label");
        }

        clarinLicenseLabelDAO.delete(context, license);
    }

    @Override
    public void update(Context context, ClarinLicenseLabel newClarinLicenseLabel) throws SQLException,
            AuthorizeException {
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an CLARIN License Label");
        }

        if (Objects.isNull(newClarinLicenseLabel)) {
            throw new NullArgumentException("Cannot update licenseLabel because the clarinLicenseLabel is null");
        }

        ClarinLicenseLabel foundClarinLicenseLabel = find(context, newClarinLicenseLabel.getID());
        if (Objects.isNull(foundClarinLicenseLabel)) {
            throw new ObjectNotFoundException(newClarinLicenseLabel.getID(), "Cannot update the clarinLicenseLabel " +
                    "because the licenseLabel wasn't found in the database.");
        }

        clarinLicenseLabelDAO.save(context, newClarinLicenseLabel);
    }
}
