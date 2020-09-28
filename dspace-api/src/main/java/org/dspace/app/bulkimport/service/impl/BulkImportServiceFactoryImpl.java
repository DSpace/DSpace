/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service.impl;

import org.dspace.app.bulkimport.service.BulkImportService;
import org.dspace.app.bulkimport.service.BulkImportServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkImportServiceFactoryImpl extends BulkImportServiceFactory {

    @Autowired
    private BulkImportService bulkImportService;

    @Override
    public BulkImportService getBulkImportService() {
        return bulkImportService;
    }

}
