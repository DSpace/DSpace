package org.dspace.content.service.clarin;

import org.dspace.content.clarin.License;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

public interface LicenseService {

    License create(Context context, License license) throws SQLException;

    License find(Context context, int valueId) throws SQLException;

    List<License> findAll(Context context) throws SQLException;

    void delete(Context context, License license) throws SQLException;

    void update(Context context, License oldLicense, License newLicense) throws SQLException;

}
