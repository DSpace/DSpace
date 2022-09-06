package org.dspace.content.service.clarin;

import org.dspace.content.clarin.LicenseLabel;
import org.dspace.core.Context;

import java.sql.SQLException;

public interface LicenseLabelService {
    LicenseLabel create(Context context, LicenseLabel licenseLabel) throws SQLException;

    LicenseLabel find(Context context, int valueId) throws SQLException;

    void delete(Context context, LicenseLabel licenseLabel) throws SQLException;

    void update(Context context, LicenseLabel oldLicenseLabel, LicenseLabel newLicenseLabel) throws SQLException;
}
