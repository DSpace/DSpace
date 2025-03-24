/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class BulkEditServiceFactory {
    public abstract BulkEditRegisterService<DSpaceCSV> getCSVBulkEditRegisterService();
    public abstract BulkEditImportService getBulkEditImportService();

    public static BulkEditServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditServiceFactory", BulkEditServiceFactory.class);
    }
}
