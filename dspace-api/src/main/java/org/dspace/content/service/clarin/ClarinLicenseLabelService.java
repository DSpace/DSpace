package org.dspace.content.service.clarin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.core.Context;

import java.sql.SQLException;

public interface ClarinLicenseLabelService {
    ClarinLicenseLabel create(Context context) throws SQLException, AuthorizeException;

    ClarinLicenseLabel create(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException,
            AuthorizeException;

    ClarinLicenseLabel find(Context context, int valueId) throws SQLException;

    void delete(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException;

    void update(Context context, ClarinLicenseLabel newClarinLicenseLabel) throws SQLException;
}
