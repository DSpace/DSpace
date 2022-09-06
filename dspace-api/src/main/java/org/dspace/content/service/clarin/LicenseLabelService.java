package org.dspace.content.service.clarin;

import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.core.Context;

import java.sql.SQLException;

public interface LicenseLabelService {
    ClarinLicenseLabel create(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException;

    ClarinLicenseLabel find(Context context, int valueId) throws SQLException;

    void delete(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException;

    void update(Context context, ClarinLicenseLabel oldClarinLicenseLabel, ClarinLicenseLabel newClarinLicenseLabel) throws SQLException;
}
