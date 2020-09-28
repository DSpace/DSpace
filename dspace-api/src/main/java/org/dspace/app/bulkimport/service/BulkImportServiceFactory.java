/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class BulkImportServiceFactory {

    public abstract BulkImportService getBulkImportService();

    public static BulkImportServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkImportServiceFactory", BulkImportServiceFactory.class);
    }
}
