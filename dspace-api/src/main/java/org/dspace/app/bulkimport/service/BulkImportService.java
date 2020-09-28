/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import java.io.InputStream;

import org.dspace.app.bulkimport.model.ImportParams;
import org.dspace.core.Context;

public interface BulkImportService {

    public void performImport(Context context, InputStream is, ImportParams params);
}
