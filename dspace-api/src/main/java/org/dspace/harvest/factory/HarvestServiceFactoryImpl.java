/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.factory;

import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.service.HarvestSchedulingService;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the harvest package, use HarvestServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestServiceFactoryImpl extends HarvestServiceFactory {

    @Autowired(required = true)
    private HarvestedItemService harvestedItemService;
    @Autowired(required = true)
    private HarvestedCollectionService harvestedCollectionService;
    @Autowired(required = true)
    private HarvestSchedulingService harvestSchedulingService;
    @Autowired(required = true)
    private OAIHarvesterClient oaiHarvesterClient;
    @Autowired(required = true)
    private OAIHarvester oaiHarvester;

    @Override
    public HarvestedCollectionService getHarvestedCollectionService() {
        return harvestedCollectionService;
    }

    @Override
    public HarvestedItemService getHarvestedItemService() {
        return harvestedItemService;
    }

    @Override
    public HarvestSchedulingService getHarvestSchedulingService() {
        return harvestSchedulingService;
    }

    @Override
    public OAIHarvesterClient getOAIHarvesterClient() {
        return oaiHarvesterClient;
    }

    @Override
    public OAIHarvester getOAIHarvester() {
        return oaiHarvester;
    }
}
