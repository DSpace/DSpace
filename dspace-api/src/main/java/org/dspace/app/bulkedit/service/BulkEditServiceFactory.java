package org.dspace.app.bulkedit.service;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class BulkEditServiceFactory {
    public abstract BulkEditRegisterService<DSpaceCSV> getCSVBulkEditRegisterService();
    public abstract CSVBulkEditCacheService getCSVBulkEditCacheService();
    public abstract BulkEditImportService getBulkEditImportService();

    public static BulkEditServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditServiceFactory", BulkEditServiceFactory.class);
    }
}
