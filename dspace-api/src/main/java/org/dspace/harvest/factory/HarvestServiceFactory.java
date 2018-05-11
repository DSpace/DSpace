/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.factory;

import org.dspace.harvest.service.HarvestSchedulingService;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the harvest package, use HarvestServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class HarvestServiceFactory {

    public abstract HarvestedCollectionService getHarvestedCollectionService();

    public abstract HarvestedItemService getHarvestedItemService();

    public abstract HarvestSchedulingService getHarvestSchedulingService();

    public static HarvestServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("harvestServiceFactory", HarvestServiceFactory.class);
    }
}
