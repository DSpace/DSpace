package org.dspace.app.bulkedit.service;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditServiceFactoryImpl extends BulkEditServiceFactory {
    @Autowired
    private BulkEditRegisterService<DSpaceCSV> csvBulkEditRegisterService;

    @Autowired
    private CSVBulkEditCacheService csvBulkEditCacheService;

    @Autowired
    private BulkEditImportService bulkEditImportService;

    @Override
    public BulkEditRegisterService<DSpaceCSV> getCSVBulkEditRegisterService() {
        return csvBulkEditRegisterService;
    }

    @Override
    public CSVBulkEditCacheService getCSVBulkEditCacheService() {
        return csvBulkEditCacheService;
    }

    @Override
    public BulkEditImportService getBulkEditImportService() {
        return bulkEditImportService;
    }
}
