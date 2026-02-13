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
 * Factory for services related to bulk-edit
 * These services cannot and should not be Autowired and always retrieved through this factory because they are stateful
 * This means every call to retrieve a service will create a new instance of that service, ensuring any variable
 * within these services remains within the context of where the service was retrieved (e.g. within the same bulk-edit)
 */
public abstract class BulkEditServiceFactory {
    /**
     * Get the service for parsing a {@link DSpaceCSV} to a list of {@link org.dspace.app.bulkedit.BulkEditChange}
     */
    public abstract BulkEditParsingService<DSpaceCSV> getCSVBulkEditParsingService();

    /**
     * Get the service for applying {@link org.dspace.app.bulkedit.BulkEditChange}s
     */
    public abstract BulkEditService getBulkEditService();

    /**
     * Get the instance of this factory to retrieve service instances from
     */
    public static BulkEditServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditServiceFactory", BulkEditServiceFactory.class);
    }
}
