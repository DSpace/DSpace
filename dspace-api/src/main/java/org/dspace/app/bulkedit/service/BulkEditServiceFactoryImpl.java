/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditServiceFactoryImpl extends BulkEditServiceFactory {
    @Autowired
    private BulkEditRegisterService<DSpaceCSV> csvBulkEditRegisterService;

    @Autowired
    private BulkEditImportService bulkEditImportService;

    @Override
    public BulkEditRegisterService<DSpaceCSV> getCSVBulkEditRegisterService() {
        return csvBulkEditRegisterService;
    }

    @Override
    public BulkEditImportService getBulkEditImportService() {
        return bulkEditImportService;
    }
}
