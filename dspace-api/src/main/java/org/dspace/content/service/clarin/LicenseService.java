package org.dspace.content.service.clarin;

import org.dspace.content.clarin.ClarinLicense;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

public interface LicenseService {

    ClarinLicense create(Context context, ClarinLicense clarinLicense) throws SQLException;

    ClarinLicense find(Context context, int valueId) throws SQLException;

    List<ClarinLicense> findAll(Context context) throws SQLException;

    void delete(Context context, ClarinLicense clarinLicense) throws SQLException;

    void update(Context context, ClarinLicense oldClarinLicense, ClarinLicense newClarinLicense) throws SQLException;

}
