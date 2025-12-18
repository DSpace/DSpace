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

/**
 * Implementation of {@link BulkEditServiceFactory}
 */
public class BulkEditServiceFactoryImpl extends BulkEditServiceFactory {
    @Override
    public BulkEditParsingService<DSpaceCSV> getCSVBulkEditParsingService() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("csvBulkEditParsingService", CSVBulkEditParsingServiceImpl.class);
    }

    @Override
    public BulkEditService getBulkEditService() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditService", BulkEditService.class);
    }
}
