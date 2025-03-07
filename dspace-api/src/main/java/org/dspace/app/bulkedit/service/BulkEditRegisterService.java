package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public interface BulkEditRegisterService<T> {
    List<BulkEditChange> registerBulkEditChange(Context context, T line)
        throws MetadataImportException, SQLException, AuthorizeException, IOException;
}
